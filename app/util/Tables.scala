package util

import models.{Players, RoomPlayer, Rooms}
import slick.lifted.TableQuery

object Tables {
  def roomsD(autoInc: Boolean): TableQuery[Rooms] = TableQuery[Rooms](tag => new Rooms(tag, autoInc))
  def playersD(autoInc: Boolean): TableQuery[Players] = TableQuery[Players](tag => new Players(tag, autoInc))
  def roomPlayerD(autoInc: Boolean): TableQuery[RoomPlayer] = TableQuery[RoomPlayer](tag => new RoomPlayer(tag, autoInc))

  val rooms = roomsD(true)
  val players = playersD(true)
  val roomPlayer = roomPlayerD(true)
}
