package models

import slick.lifted.TableQuery

object Tables {
  val meta = TableQuery[Meta]
  val players = TableQuery[Players]
  val roomPlayer = TableQuery[RoomPlayer]
}
