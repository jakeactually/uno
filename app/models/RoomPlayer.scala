package models

import slick.jdbc.SQLiteProfile.api._
import slick.lifted.Tag

case class RoomPlayerItem(id: Int, room: Int, player: Int)

class RoomPlayer(tag: Tag) extends Table[RoomPlayerItem](tag, "room_player") {
  def id = column[Int]("rowid", O.PrimaryKey, O.AutoInc)
  def room = column[Int]("room")
  def player = column[Int]("player")
  def * = (id, room, player).mapTo[RoomPlayerItem]
}
