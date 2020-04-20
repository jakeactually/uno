package controllers

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.stream.Materializer
import db.AppDb
import javax.inject._
import models.{Card, CardColor, Cards, ChangeColor, Player, Plus4, Red, Room}
import play.api.libs.streams.ActorFlow
import play.api.mvc._

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import play.api.libs.json.Json
import views.html.room

import scala.async.Async._

@Singleton
class GameController @Inject()
(val controllerComponents: ControllerComponents, val appDb: AppDb)
(implicit system: ActorSystem, mat: Materializer) extends UnoController {

  import appDb._

  val playersNotifier: mutable.Map[Int, Seq[ActorRef]] = mutable.Map()

  def turn(roomId: Int): Action[AnyContent] = Action.async { request =>
    turnContext(request, roomId) { player =>
      async {
        val cardId = request.body.asFormUrlEncoded.get("cardId").head.toInt
        val topCardId = await(roomsDb.top(roomId))

        val card1 = Cards.of(topCardId)
        val card2 = Cards.of(cardId)
        println(card1)
        println(card2)
        println()

        val chosenColor = if (card1.isColorCard)
          await(roomsDb.getColor(roomId))
        else
          Red

        val roomState = await(roomsDb.getGameState(roomId))

        card1.matches(roomState, chosenColor, card2) match {
          case Some(error) => BadRequest(error)
          case None =>
            await(doTurn(roomId, player.id, cardId))
            await(effects(request, roomId, card2))
            theEnd(roomId)
        }
      }
    }
  }

  def doTurn(roomId: Int, playerId: Int, cardId: Int): Future[Unit] = for {
    _ <- playersDb.removeCard(playerId, cardId)
    _ <- roomsDb.push(roomId, cardId)
    _ <- roomsDb.next(roomId)
  } yield ()

  def effects(request: Request[AnyContent], roomId: Int, card: Card): Future[Unit] = for {
    _ <- where (card.isColorCard) {
      val newColor = CardColor.fromStr(request.body.asFormUrlEncoded.get("color").head)
      roomsDb.setColor(roomId, newColor.get)
    }

    _ <- where (card.canChain) {
      for {
        _ <- roomsDb.setGameState(roomId, Some(card.toGameState))
        _ <- roomsDb.increaseChainCount(roomId)
      } yield ()
    }

    _ <- where (card.isReverse) {
      roomsDb.changeDirection(roomId)
    }
  } yield ()

  def where(condition: Boolean)(future: => Future[Unit]): Future[Unit] = {
    if (condition)
      future
    else
      Future.successful(())
  }

  def theEnd(roomId: Int): Status = {
    playersNotifier(roomId) = playersNotifier.getOrElse(roomId, Seq())
    playersNotifier(roomId).foreach(_ ! "update")
    Ok
  }

  def game(roomId: Int): WebSocket = WebSocket.accept[String, String] { request =>
    ActorFlow.actorRef { out =>
      playersNotifier(roomId) = playersNotifier.getOrElse(roomId, Seq())
      playersNotifier(roomId) :+= out
      Props(new PlayerActor(out))
    }
  }

  def draw(roomId: Int): Action[AnyContent] = Action.async { request =>
    context(request, roomId) { player =>
      for {
        card <- roomsDb.draw(roomId)
        _ <- playersDb.push(player.id, card)
        _ <- playersDb.setDrawed(player.id, true)
      } yield Redirect(routes.SetupController.start(roomId))
    }
  }

  def boardState(roomId: Int): Action[AnyContent] = Action.async { request =>
    context(request, roomId) { player =>
      for {
        drawed <- playersDb.getDrawed(player.id)
        r <- roomsDb.get(roomId)
        isTurn <- roomsDb.isTurn(roomId, player.id)
      } yield Ok(Json.toJson((isTurn, drawed, r.color, r.gameState, r.chainCount)))
    }
  }

  def pass(roomId: Int): Action[AnyContent] = Action.async { request =>
    turnContext(request, roomId) { player =>
      if (player.drawed)
        for {
          _ <- roomsDb.next(roomId)
          _ <- playersDb.setDrawed(player.id, false)
        } yield theEnd(roomId)
      else
        Future.successful(BadRequest("You must draw one card"))
    }
  }

  def penalty(roomId: Int): Action[AnyContent] = Action.async { request =>
    turnContext(request, roomId) { player =>
      roomsDb.get(roomId) flatMap { room =>
        val dbResult = if (room.gameState.getOrElse("") == "plus2") {
          penalty(room, player, 2 * room.chainCount)
        } else if (room.gameState.getOrElse("") == "plus4") {
          penalty(room, player, 4 * room.chainCount)
        } else if (room.gameState.getOrElse("") == "stop") {
          roomsDb.setGameState(room.id, None) flatMap { _ => roomsDb.next(room.id) }
        } else {
          Future.successful(())
        }

        dbResult map { _ => theEnd(roomId) }
      }
    }
  }

  def penalty(room: Room, player: Player, amount: Int): Future[Unit] = for {
    cs <- roomsDb.drawMany(room.id, amount)
    _ <- playersDb.pushMany(player.id, cs)
    _ <- roomsDb.setGameState(room.id, None)
    _ <- roomsDb.setChainCount(room.id, 0)
    _ <- roomsDb.next(room.id)
  } yield ()

  class PlayerActor(val out: ActorRef) extends Actor {
    def receive: Receive = _ => ()
  }

}
