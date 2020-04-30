package models

import slick.jdbc.SQLiteProfile.api._
import slick.lifted.Tag

case class Player(id: Int, name: String, cards: String, drawed: Boolean)

class Players(tag: Tag, autoInc: Boolean) extends Table[Player](tag, "players") {
  def id = if (autoInc)
    column[Int]("rowid", O.PrimaryKey, O.AutoInc)
  else
    column[Int]("rowid", O.PrimaryKey)

  def name = column[String]("name")
  def cards = column[String]("cards")
  def drawed = column[Boolean]("drawed")
  def * = (id, name, cards, drawed).mapTo[Player]
}
