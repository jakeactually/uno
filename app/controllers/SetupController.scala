package controllers

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.stream.Materializer
import db.AppDb
import javax.inject._
import models.Cards
import play.api.libs.json.Json
import play.api.libs.streams.ActorFlow
import play.api.mvc._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import slick.jdbc.H2Profile.api._

import scala.collection.mutable
import scala.util.Random

@Singleton
class GameController @Inject()(val controllerComponents: ControllerComponents, val appDb: AppDb)(implicit system: ActorSystem, mat: Materializer) extends BaseController {

  val playersNotifier: mutable.Map[Int, Seq[ActorRef]] = mutable.Map()

  import appDb._

  def start(roomId: Int): Action[AnyContent] = Action.async { request =>
    roomsDb.allPlayers(roomId) flatMap { ps =>
      if (ps.length < 2) {
        Future.successful(BadRequest("Not enough players"))
      } else {
        playersDb.userOrNew(request).flatMap(roomsDb.inRoom(roomId, _)) flatMap { invited =>
          if (invited)
            doPlay(request, roomId)
          else
            Future.successful(Unauthorized("You haven't joined this room"))
        }
      }
    }
  }

  def doPlay(request: Request[AnyContent], roomId: Int): Future[Result] = {
    roomsDb.getActive(roomId) flatMap { active =>
      if (active) {
        Future.successful(Ok(views.html.game(roomId)))
      } else {
        for {
          _ <- deal(roomId)
          top <- roomsDb.draw(roomId)
          _ <- roomsDb.setCenter(roomId, Seq(top))
          _ <- roomsDb.setActive(roomId, active = true)
        } yield {
          Ok(views.html.game(roomId))
        }
      }
    }
  }

  def deal(roomId: Int): Future[Int] = roomsDb.allPlayers(roomId) flatMap { ps =>
    var allCards = shuffle(Cards.all.map(identity).zipWithIndex.toArray).toSeq

    val io = ps.map { p =>
      val (x, xs) = allCards.splitAt(7)
      allCards = xs
      playersDb.setCardsRaw(p.id, x.map(_._2))
    }

    db.run { DBIO.seq(io:_*) } flatMap { _ =>
      roomsDb.setDeck(roomId, allCards.map(_._2))
    }
  }

  def shuffle[T](array: Array[T]): Array[T] = {
    for (_ <- 0 to array.length) {
      val a = Random.nextInt(array.length)
      val b = Random.nextInt(array.length)
      val temp = array(a)
      array(a) = array(b)
      array(b) = temp
    }

    array
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
  } yield cards map (_.toInt)

  def center(roomId: Int): Action[AnyContent] = Action.async {
    roomsDb.getCenter(roomId) map { c =>
      Ok(Json.toJson(c map toCard))
    }
  }

  def game(roomId: Int): WebSocket = WebSocket.accept[String, String] { request =>
    ActorFlow.actorRef { out =>
      playersNotifier(roomId) = playersNotifier.getOrElse(roomId, Seq())
      playersNotifier(roomId) :+= out
      Props(new PlayerActor(out))
    }
  }

  class PlayerActor(val out: ActorRef) extends Actor {
    def receive: Receive = {
      case str: String => {
        (str.substring(0, 4), str.substring(5))  match {
          case ("card", n) => ()
        }
      }
    }
  }
}
