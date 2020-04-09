package models

object Cards {
  val colors = Seq(Red, Green, Blue, Yellow)

  val all: Seq[Card] =
    colors.map { Number(0, _) } ++
    { for { n <- 1 to 9; c <- colors; _ <- 0 to 1 } yield Number(n, c) } ++
    colors.map(Stop) ++
    colors.map(Reverse) ++
    colors.map(Plus2) ++
    List.fill(4)(ChangeColor) ++
    List.fill(4)(Plus4)
}
