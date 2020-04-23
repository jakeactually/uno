package models

import slick.jdbc.SQLiteProfile.api._
import slick.lifted.Tag
import util.Constants

case class Room(id: Int,
                active: Boolean,
                deck: String,
                center: String,
                turn: Int,
                direction: Boolean,
                color: String,
                gameState: Option[String],
                chainCount: Int)

object RoomCompanion {
  def newRoom: Room = Room(0, active = false, "", "", 0, direction = false, "red", None, 0)

  def ofId(id: Int): Room = Room(id, active = false, "", "", 0, direction = false, "red", None, 0)
}

class Rooms(tag: Tag) extends Table[Room](tag, "rooms") {
  def id = if (Constants.autoInc)
    column[Int]("rowid", O.PrimaryKey, O.AutoInc)
  else
    column[Int]("rowid", O.PrimaryKey)

  def active = column[Boolean]("active")
  def deck = column[String]("deck")
  def center = column[String]("center")
  def turn = column[Int]("turn")
  def direction = column[Boolean]("direction")
  def color = column[String]("color")
  def gameState = column[Option[String]]("game_state")
  def chainCount = column[Int]("chain_count")
  def * = (id, active, deck, center, turn, direction, color, gameState, chainCount).mapTo[Room]
}
