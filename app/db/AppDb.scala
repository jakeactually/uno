package db

import models.{MetaItem, Tables}
import slick.jdbc.H2Profile.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AppDb {

  val db = Database.forConfig("sqlite")
  val roomsDb = new Room(db)
  val playersDb = new Player(db)

  def init: Future[Int] = db.run {
    val schemas =
      Tables.meta.schema ++
      Tables.roomPlayer.schema ++
      Tables.players.schema

    schemas.createIfNotExists
  } flatMap { _ =>
    db.run {
      Tables.meta += MetaItem(0, "roomsCount", "0")
    }
  }

}
