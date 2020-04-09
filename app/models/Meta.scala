package models

import slick.jdbc.SQLiteProfile.api._
import slick.lifted.Tag

case class MetaItem(id: Int, metaKey: String, metaValue: String)

class Meta(tag: Tag) extends Table[MetaItem](tag, "meta") {
  def id = column[Int]("rowid", O.PrimaryKey, O.AutoInc)
  def metaKey = column[String]("meta_key")
  def metaValue = column[String]("meta_value")
  def * = (id, metaKey, metaValue).mapTo[MetaItem]
}
