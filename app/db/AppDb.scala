package db

import models.Tables
import slick.jdbc.H2Profile.api._

import scala.concurrent.Future

class AppDb {

  val db = Database.forConfig("sqlite")
  val roomsDb = new RoomDb(db)
  val playersDb = new PlayerDb(db)

  def init: Future[Unit] = db.run {
    val schemas =
      Tables.rooms.schema ++
      Tables.roomPlayer.schema ++
      Tables.players.schema

    schemas.createIfNotExists
  }

}
