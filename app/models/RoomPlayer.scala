package models

import slick.jdbc.SQLiteProfile.api._
import slick.lifted.Tag

case class RoomPlayerItem(id: Int, room: Int, player: Int)

class RoomPlayer(tag: Tag, autoInc: Boolean) extends Table[RoomPlayerItem](tag, "room_player") {
  def id = if (autoInc)
    column[Int]("rowid", O.PrimaryKey, O.AutoInc)
  else
    column[Int]("rowid", O.PrimaryKey)

  def room = column[Int]("room")
  def player = column[Int]("player")
  def * = (id, room, player).mapTo[RoomPlayerItem]
}
