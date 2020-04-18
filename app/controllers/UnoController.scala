package controllers

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.stream.Materializer
import db.AppDb
import javax.inject._
import play.api.libs.streams.ActorFlow
import play.api.mvc._

import scala.collection.mutable
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class HomeController @Inject()(val controllerComponents: ControllerComponents, val appDb: AppDb)(implicit system: ActorSystem, mat: Materializer) extends BaseController {

  val playersNotifier: mutable.Map[Int, Seq[ActorRef]] = mutable.Map()

  import appDb._

  def init: Action[AnyContent] = Action.async {
    appDb.init map { _ =>
      Ok("Ok")
    }
  }

  def index: Action[AnyContent] = Action.async { request =>
    playersDb.username(request).map { un =>
      Ok(views.html.index(un))
    }
  }

  def newRoom(): Action[AnyContent] = Action.async { request =>
    roomsDb.next.flatMap { rid =>
      playersNotifier(rid) = Seq()
      joinRoom(request, rid)
    }
  }

  def joinRoom(request: Request[AnyContent], roomId: Int): Future[Result] = for {
    uid <- playersDb.userOrNew(request)
    _ <- playersDb.setName(uid, request.body.asFormUrlEncoded.get("username").head)
    _ <- roomsDb.join(roomId, uid)
  } yield {
    playersNotifier(roomId) = playersNotifier.getOrElse(roomId, Seq())
    playersNotifier(roomId).foreach(_ ! "update")
    Redirect(routes.HomeController.room(roomId)).withSession("userId" -> uid.toString)
  }

  def joinRoomGet(roomId: Int): Action[AnyContent] = Action.async { request =>
    playersDb.username(request).map { un =>
      Ok(views.html.join(roomId, un))
    }
  }

  def joinRoomPost(roomId: Int): Action[AnyContent] = Action.async { request =>
    joinRoom(request, roomId)
  }

  def room(id: Int): Action[AnyContent] = Action.async { request =>
    for {
      uid <- playersDb.userOrNew(request)
      invited <- roomsDb.inRoom(id, uid)
      r <- if (invited)
          roomsDb.allPlayers(id) flatMap { ps =>
            Future.successful(Ok(views.html.room(id, uid, ps.map(p => (p.id, p.name)))))
          }
        else
          Future.successful(Unauthorized("You haven't joined this room"))
    } yield r
  }

  def expel(roomId: Int, userId: Int): Action[AnyContent] = Action.async { request =>
    roomsDb.expel(roomId, userId).map { _ =>
      playersNotifier(roomId) = playersNotifier.getOrElse(roomId, Seq())
      playersNotifier(roomId).foreach(_ ! "update")
      Redirect(routes.HomeController.joinRoomGet(roomId))
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
