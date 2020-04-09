package controllers

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.stream.Materializer
import db.AppDb
import javax.inject._
import models.{Cards, Player}
import play.api.libs.streams.ActorFlow
import play.api.mvc._

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import slick.jdbc.H2Profile.api._

@Singleton
class GameController @Inject()(val controllerComponents: ControllerComponents, val appDb: AppDb)(implicit system: ActorSystem, mat: Materializer) extends BaseController {

  import appDb._

  val started: mutable.Set[Int] = mutable.Set()

  def play(roomId: Int): Action[AnyContent] = Action.async { request =>
    if (started.contains(roomId)) {
      for {
        uid <- playersDb.userOrNew(request)
        cards <- playersDb.getCards(uid)
      } yield Ok(cards)
    } else {
      for {
        _ <- shuffle(roomId)
        uid <- playersDb.userOrNew(request)
        cards <- playersDb.getCards(uid)
      } yield {
        started.add(roomId)
        Ok(cards)
      }
    }
  }

  def shuffle(roomId: Int): Future[Unit] = roomsDb.allPlayers(roomId) flatMap { ps =>
    var allCards = Cards.all.map(identity)

    val io = ps.map { p =>
      val (x, xs) = allCards.splitAt(7)
      allCards = xs
      playersDb.setCardsRaw(p.id, x.mkString(","))
    }

    db.run { DBIO.seq(io:_*) }
  }
}
