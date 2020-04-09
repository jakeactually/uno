package db

import models.{Player, RoomPlayerItem, Tables}
import slick.jdbc.H2Profile.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class Room(val db: Database) {

  import Tables._

  val countMeta = meta
    .filter(_.metaKey === "roomsCount")
    .map(_.metaValue)

  def getCount: Future[Int] = db.run {
    countMeta.result
  } map {
    _.head.toInt
  }

  def setCount(n: Int): Future[Int] = db.run {
    countMeta.update(n.toString)
  }

  def next: Future[Int] = for {
    n <- getCount
    _ <- setCount(n + 1)
    m <- getCount
  } yield m

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

  def expel(room: Int, player: Int): Future[Int] = db.run {
    roomPlayer.filter(rp => rp.room === room && rp.player === player).delete
  }

}
