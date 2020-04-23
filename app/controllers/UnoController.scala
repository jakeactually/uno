package controllers

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.stream.Materializer
import db.AppDb
import javax.inject._
import models.Player
import play.api.libs.streams.ActorFlow
import play.api.mvc._

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Random

@Singleton
abstract class UnoController extends BaseController {

  val appDb: AppDb

  import appDb._

  def context(request: Request[AnyContent], roomId: Int)(f: Player => Future[Result]): Future[Result] = {
    for {
      uid <- playersDb.userOrNew(request)
      invited <- roomsDb.inRoom(roomId, uid)
      r <- if (invited)
        playersDb.user(request) flatMap { o => f(o.get) }
      else
        Future.successful(Unauthorized("You haven't joined this room"))
    } yield r
  }

  def turnContext(request: Request[AnyContent], roomId: Int)(f: Player => Future[Result]): Future[Result] = {
    context(request, roomId) { player =>
      roomsDb.isTurn(roomId, player.id) flatMap {
        if (_)
          f(player)
        else
          Future.successful(BadRequest("Not your turn"))
      }
    }
  }


}
