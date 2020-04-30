package controllers

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.stream.Materializer
import db.AppDb
import javax.inject._
import play.api.libs.streams.ActorFlow
import play.api.mvc.{request, _}

import scala.async.Async._
import scala.collection.mutable
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class HomeController @Inject()
(val controllerComponents: ControllerComponents, val appDb: AppDb)
(implicit system: ActorSystem, mat: Materializer) extends UnoController {

  import appDb._

  val playersNotifier: mutable.Map[Int, Seq[ActorRef]] = mutable.Map()

  def index: Action[AnyContent] = Action.async { request =>
    appDb.init flatMap { _ =>
      playersDb.username(request).map { un =>
        Ok(views.html.index(un))
      }
    }
  }

  def newRoom(): Action[AnyContent] = Action.async { request =>
    roomsDb.next.flatMap { rid =>
      playersNotifier(rid) = Seq()
      joinRoom(request, rid)
    }
  }

  def joinRoom(request: Request[AnyContent], roomId: Int): Future[Result] = async {
    val uid = await(playersDb.userOrNew(request))
    await(playersDb.setName(uid, request.body.asFormUrlEncoded.get("username").head))
    await(roomsDb.join(roomId, uid))

    playersNotifier(roomId) = playersNotifier.getOrElse(roomId, Seq())
    playersNotifier(roomId).foreach(_ ! "update")
    Redirect(routes.HomeController.room(roomId)).withSession("userId" -> uid.toString)
  }

  def joinRoomGet(roomId: Int): Action[AnyContent] = Action.async { request =>
    async {
      if (await(roomsDb.countPlayers(roomId)) >= 8) {
        BadRequest("Only up to 8 players")
      } else {
        val active = await(roomsDb.getActive(roomId))

        if (active) {
          BadRequest("This room is already playing")
        } else {
          val un = await(playersDb.username(request))
          Ok(views.html.join(roomId, un))
        }
      }
    }
  }

  def joinRoomPost(roomId: Int): Action[AnyContent] = Action.async { request =>
    async {
      if (await(roomsDb.countPlayers(roomId)) >= 8) {
        BadRequest("Only up to 8 players")
      } else {
        val active = await(roomsDb.getActive(roomId))

        if (active) {
          BadRequest("This room is already playing")
        } else {
          await(joinRoom(request, roomId))
        }
      }
    }
  }

  def room(id: Int): Action[AnyContent] = Action.async { implicit request =>
    async {
      val active = await(roomsDb.getActive(id))

      if (active) {
        BadRequest("This room is already playing")
      } else {
        val uid = await(playersDb.userOrNew(request))
        val invited = await(roomsDb.inRoom(id, uid))

        if (invited) {
          val player = await(playersDb.user(request))
          val players = await(roomsDb.allPlayers(id))
          Ok(views.html.room(id, player.get.id, players.map(p => (p.id, p.name))))
        } else {
          Redirect(routes.HomeController.joinRoomGet(id))
        }
      }
    }
  }

  def expel(roomId: Int, userId: Int): Action[AnyContent] = Action.async { request =>
    roomsDb.expel(roomId, userId).map { _ =>
      playersNotifier(roomId) = playersNotifier.getOrElse(roomId, Seq())
      playersNotifier(roomId).foreach(_ ! "update")
      Redirect(routes.HomeController.index())
    }
  }

  def roomState(roomId: Int): WebSocket = WebSocket.accept[String, String] { request =>
    ActorFlow.actorRef { out =>
      playersNotifier(roomId) = playersNotifier.getOrElse(roomId, Seq())
      playersNotifier(roomId) :+= out
      Props(new AssistantsActor(out))
    }
  }

  class AssistantsActor(val out: ActorRef) extends Actor {
    def receive: Receive = _ => ()
  }
}
