package db

import models.{Player, Tables}
import play.api.mvc.{AnyContent, Request}
import slick.dbio.Effect
import slick.jdbc.H2Profile.api._
import slick.sql.FixedSqlAction

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class Player(val db: Database) {

  import Tables.players

  def find(id: Int): Future[Option[models.Player]] = db.run {
    players.filter(_.id === id).result
  } map {
    _.headOption
  }

  def user(request: Request[AnyContent]): Future[Option[models.Player]] = {
    request.session.get("userId") match {
      case Some(id) => find(id.toInt)
      case None => Future.successful(None)
    }
  }

  def username(request: Request[AnyContent]): Future[Option[String]] = user(request).map(_.map(_.name))

  def userOrNew(request: Request[AnyContent]): Future[Int] = {
    user(request).flatMap {
      case Some(user) => Future.successful(user.id)
      case None => newPlayer
    }
  }

  def newPlayer: Future[Int] = db.run {
    (players returning players.map(_.id)) += Player(0, "", "")
  }

  def getName(id: Int): Future[Option[String]] = find(id).map(_.map(_.name))

  def setName(userId: Int, name: String): Future[Int] = db.run {
    players.filter(_.id === userId).map(_.name).update(name)
  }

  def getCards(id: Int): Future[Option[String]] = find(id).map(_.map(_.cards))

  def setCards(userId: Int, cards: String): Future[Int] = db.run {
    players.filter(_.id === userId).map(_.cards).update(cards)
  }

  def setCardsRaw(userId: Int, cards: String): FixedSqlAction[Int, NoStream, Effect.Write] =
    players.filter(_.id === userId).map(_.cards).update(cards)
}
