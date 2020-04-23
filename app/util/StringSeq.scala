package util

object StringSeq {
  def parse(str: String): Seq[Int] = if (str == "") {
    Seq()
  } else {
    str.split(",").map(_.toInt).toSeq
  }
}
