package db

import slick.jdbc.H2Profile.api._
import util.Tables

import scala.concurrent.Future

class AppDb {

  val db = Database.forConfig("sqlite")
  val roomsDb = new RoomDb(db)
  val playersDb = new PlayerDb(db)

  def init: Future[Unit] = db.run {
    val schemas =
      Tables.roomsD(false).schema ++
      Tables.roomPlayerD(false).schema ++
      Tables.playersD(false).schema

    schemas.createIfNotExists
  }

}
