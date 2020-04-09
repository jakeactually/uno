package models

import slick.jdbc.SQLiteProfile.api._
import slick.lifted.Tag

case class Player(id: Int, name: String, cards: String)

class Players(tag: Tag) extends Table[Player](tag, "players") {
  def id = column[Int]("rowid", O.PrimaryKey, O.AutoInc)
  def name = column[String]("name")
  def cards = column[String]("cards")
  def * = (id, name, cards).mapTo[Player]
}
