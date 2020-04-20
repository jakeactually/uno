package models

import slick.lifted.TableQuery

object Tables {
  val rooms = TableQuery[Rooms]
  val players = TableQuery[Players]
  val roomPlayer = TableQuery[RoomPlayer]
}
