package controllers

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.stream.Materializer
import db.AppDb
import javax.inject._
import models.{Room, RoomCompanion}
import play.api.libs.json.Json
import play.api.libs.streams.ActorFlow
import play.api.mvc._
import slick.jdbc.H2Profile.api._
import util.{Cards, Shuffle}

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Random

@Singleton
class SetupController @Inject()
(val controllerComponents: ControllerComponents, val appDb: AppDb)
(implicit system: ActorSystem, mat: Materializer) extends UnoController {

  import appDb._

  def start(roomId: Int): Action[AnyContent] = Action.async { implicit request =>
    context(request, roomId) { _ =>
      roomsDb.allPlayers(roomId) flatMap { ps =>
        if (ps.length < 2)
          Future.successful(BadRequest("Not enough players"))
        else
          doPlay(roomId)
      }
    }
  }

  def doPlay(roomId: Int)(implicit request: RequestHeader): Future[Result] = {
    roomsDb.getActive(roomId) flatMap { active =>
      if (active) {
        Future.successful(Ok(views.html.game(roomId)))
      } else {
        for {
          _ <- roomsDb.set(roomId, RoomCompanion.ofId(roomId))
          _ <- deal(roomId)
          top <- normalTop(roomId)
          _ <- roomsDb.setCenter(roomId, Seq(top))
          _ <- roomsDb.setActive(roomId, active = true)
        } yield {
          Ok(views.html.game(roomId))
        }
      }
    }
  }

  def normalTop(roomId: Int): Future[Int] = roomsDb.draw(roomId) flatMap { topId =>
    if (Cards.of(topId).isNormal) {
      Future.successful(topId)
    } else {
      for {
        deck <- roomsDb.getDeck(roomId: Int)
        _ <- roomsDb.setDeck(roomId, deck :+ topId)
        newTop <- normalTop(roomId)
      } yield newTop
    }
  }

  def deal(roomId: Int): Future[Int] = roomsDb.allPlayers(roomId) flatMap { ps =>
    var allCards = Shuffle.shuffle(Cards.all.map(identity).zipWithIndex.toArray).toSeq

    val io = ps.map { p =>
      val (x, xs) = allCards.splitAt(7)
      allCards = xs
      playersDb.setCardsRaw(p.id, x.map(_._2))
    }

    db.run { DBIO.seq(io:_*) } flatMap { _ =>
      roomsDb.setDeck(roomId, allCards.map(_._2))
    }
  }

  def hand: Action[AnyContent] = Action.async { request =>
    playerData(request) map { cards =>
      Ok(Json.toJson(cards map toCard))
    }
  }

  def toCard(cardId: Int): (Int, String) = (cardId.toInt, Cards.all(cardId.toInt).toStr)

  def playerData(request: Request[AnyContent]): Future[Seq[Int]] = for {
    uid <- playersDb.user(request)
    cards <- playersDb.getCards(uid.get.id)
  } yield cards

  def center(roomId: Int): Action[AnyContent] = Action.async {
    roomsDb.getCenter(roomId) map { c =>
      Ok(Json.toJson(c map toCard))
    }
  }

  def gameOver(roomId: Int): Action[AnyContent] = Action.async { request =>
    roomsDb.setActive(roomId, false) map { _ =>
      Ok(views.html.theEnd(roomId))
    }
  }
}
