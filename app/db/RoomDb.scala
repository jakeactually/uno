package db

import models.{Player, Room, RoomPlayerItem, Rooms, Tables}
import slick.jdbc.H2Profile.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import models.CardColor
import models.Cards
import play.api.mvc.{AnyContent, Request}

class RoomDb(val db: Database) {

  import Tables._

  def getCount: Future[Int] = db.run { rooms.length.result }

  def next: Future[Int] = db.run {
    (rooms returning rooms.map(_.id)) += Room(0, active = false, "", "", 0, direction = false, "red", None, 0)
  }

  def get(roomId: Int): Future[Room] = db.run {
    rooms.filter(_.id === roomId).result.head
  }

  def set(roomId: Int, room: Room): Future[Int] = db.run {
    rooms.filter(_.id === roomId).update(room)
  }

  def getActive(roomId: Int): Future[Boolean] = get(roomId).map(_.active)
  def getDeck(roomId: Int): Future[Seq[Int]] = get(roomId).map(_.deck.split(",").map(_.toInt))
  def getCenter(roomId: Int): Future[Seq[Int]] = get(roomId).map(_.center.split(",").map(_.toInt))
  def getTurn(roomId: Int): Future[Int] = get(roomId).map(_.turn)
  def getDirection(roomId: Int): Future[Boolean] = get(roomId).map(_.direction)
  def getColor(roomId: Int): Future[CardColor] = get(roomId).map(c => CardColor.fromStr(c.color).get)
  def getGameState(roomId: Int): Future[Option[String]] = get(roomId).map(_.gameState)
  def getChainCount(roomId: Int): Future[Int] = get(roomId).map(_.chainCount)

  def draw(roomId: Int): Future[Int] = drawMany(roomId, 1).map(_.head)

  def drawMany(roomId: Int, amount: Int): Future[Seq[Int]] = for {
    deck <- getDeck(roomId)
    (x, xs) = deck.splitAt(amount)
    _ <- setDeck(roomId, xs)
  } yield x

  def push(roomId: Int, card: Int): Future[Unit] = for {
    center <- getCenter(roomId)
    _ <- setCenter(roomId, center :+ card)
  } yield ()

  def theRoom(roomId: Int): Query[Rooms, Room, Seq] = rooms.filter(_.id === roomId)

  def setActive(roomId: Int, active: Boolean): Future[Int] = db.run {
    theRoom(roomId).map(_.active).update(active)
  }

  def setDeck(roomId: Int, deck: Seq[Int]): Future[Int] = db.run {
    theRoom(roomId).map(_.deck).update(deck.mkString(","))
  }

  def setCenter(roomId: Int, center: Seq[Int]): Future[Int] = db.run {
    theRoom(roomId).map(_.center).update(center.mkString(","))
  }

  def setTurn(roomId: Int, turn: Int): Future[Int] = db.run {
    theRoom(roomId).map(_.turn).update(turn)
  }

  def setDirection(roomId: Int, direction: Boolean): Future[Int] = db.run {
    theRoom(roomId).map(_.direction).update(direction)
  }

  def setColor(roomId: Int, color: CardColor): Future[Unit] = db.run {
    theRoom(roomId).map(_.color).update(color.toStr).map(_ => ())
  }

  def setGameState(roomId: Int, gameState: Option[String]): Future[Unit] = db.run {
    theRoom(roomId).map(_.gameState).update(gameState).map(_ => ())
  }

  def setChainCount(roomId: Int, chainCount: Int): Future[Int] = db.run {
    theRoom(roomId).map(_.chainCount).update(chainCount)
  }

  def increaseChainCount(roomId: Int): Future[Unit] = for {
    cc <- getChainCount(roomId)
    _ <- setChainCount(roomId, cc + 1)
  } yield ()

  def join(room: Int, player: Int): Future[Int] = db.run {
    roomPlayer.filter(rp => rp.room === room && rp.player === player).result.headOption
  } flatMap {
    case Some(_) => db.run { DBIO.successful(0) }
    case None => db.run { roomPlayer += RoomPlayerItem(0, room, player) }
  }

  def allPlayers(room: Int): Future[Seq[Player]] = db.run {
    {
      for {
        rp <- roomPlayer
        p <- players if rp.room === room && rp.player === p.id
      } yield p
    }.result
  }

  def countPlayers(room: Int): Future[Int] = db.run {
    {
      for {
        rp <- roomPlayer
        p <- players if rp.room === room && rp.player === p.id
      } yield p
    }.length.result
  }

  def expel(room: Int, player: Int): Future[Int] = db.run {
    roomPlayer.filter(rp => rp.room === room && rp.player === player).delete
  }

  def inRoom(room: Int, player: Int): Future[Boolean] = db.run {
    roomPlayer.filter(rp => rp.room === room && rp.player === player).result.headOption
  } map {
    _.isDefined
  }

  def isTurn(roomId: Int, player: Int): Future[Boolean] = for {
    t <- getTurn(roomId)
    p <- allPlayers(roomId)
  } yield p(t).id == player

  def left(roomId: Int): Future[Unit] = for {
    c <- countPlayers(roomId)
    t <- getTurn(roomId)
    next = if (t == 0) c - 1 else t - 1
    _ <- setTurn(roomId, next)
  } yield ()

  def right(roomId: Int): Future[Unit] = for {
    c <- countPlayers(roomId)
    t <- getTurn(roomId)
    next = if (t == c - 1) 0 else t + 1
    _ <- setTurn(roomId, next)
  } yield ()

  def next(roomId: Int): Future[Unit] = for {
    dir <- getDirection(roomId)
    r <- if (dir) right(roomId) else left(roomId)
  } yield r

  def skip(roomId: Int): Future[Unit] = next(roomId) flatMap { _ => next(roomId) }

  def top(roomId: Int): Future[Int] = getCenter(roomId).map(_.last)

  def changeDirection(roomId: Int): Future[Unit] = for {
    d <- getDirection(roomId)
    _ <- setDirection(roomId, !d)
  } yield ()
}
