package models

sealed class CardColor {
  def toChar: String = this match {
    case Red => "r"
    case Green => "g"
    case Blue => "b"
    case Yellow => "y"
  }

  def toStr: String = this match {
    case Red => "red"
    case Green => "green"
    case Blue => "blue"
    case Yellow => "yellow"
  }
}

object CardColor {
  def fromStr(str: String): Option[CardColor] = str match {
    case "red" => Some(Red)
    case "green" => Some(Green)
    case "blue" => Some(Blue)
    case "yellow" => Some(Yellow)
    case _ => None
  }
}

case object Red extends CardColor
case object Green extends CardColor
case object Blue extends CardColor
case object Yellow extends CardColor

sealed class Card {
  def toStr: String = this match {
    case Number(number, color) => color.toChar + number
    case Stop(color) => color.toChar + "-stop"
    case Reverse(color) => color.toChar + "-reverse"
    case Plus2(color) => color.toChar + "-plus2"
    case ChangeColor => "color"
    case Plus4 => "plus4"
  }

  def matches(gameState: Option[String], choosenColor: CardColor, other: Card): Option[String] = {
    gameState match {
      case Some(state) => state match {
        case "stop" => mkErr(other.isStop)("You're blocked")
        case "plus2" => mkErr(other.isPlus2)("You're blocked")
        case "plus4" => mkErr(other == Plus4)("You're blocked")
      }
      case None => freeMatch(choosenColor, other)
    }
  }

  def freeMatch(choosenColor: CardColor, other: Card): Option[String] = {
    this match {
      case Number(number, color) => other match {
        case Number(number2, color2) => mkErr(number == number2 || color == color2)("Invalid Move")
        case _ => mkErr(other.getColor.forall(_ == color))("Wrong color")
      }
      case Stop(color) => mkErr(other.isStop || other.getColor.forall(_ == color))("Wrong color")
      case Reverse(color) => mkErr(other.isReverse || other.getColor.forall(_ == color))("Wrong color")
      case Plus2(color) => mkErr(other.isPlus2 || other.getColor.forall(_ == color))("Wrong color")
      case ChangeColor => mkErr(other.getColor.forall(_ == choosenColor))("Not chosen color")
      case Plus4 => mkErr(other.getColor.forall(_ == choosenColor))("Not chosen color")
    }
  }

  def mkErr(condition: Boolean)(msg: String): Option[String] = if (condition) None else Some(msg)

  def isStop: Boolean = this match {
    case Stop(_) => true
    case _ => false
  }

  def isReverse: Boolean = this match {
    case Reverse(_) => true
    case _ => false
  }

  def isPlus2: Boolean = this match {
    case Plus2(_) => true
    case _ => false
  }

  def getColor: Option[CardColor] = this match {
    case Number(_, color) => Some(color)
    case Stop(color) => Some(color)
    case Reverse(color) => Some(color)
    case Plus2(color) => Some(color)
    case _ => None
  }

  def isColorCard: Boolean = this == Plus4 || this == ChangeColor
  def canChain: Boolean = isPlus2 || isStop || this == Plus4

  def toGameState: String = this match {
    case Stop(_) => "stop"
    case Plus2(_) => "plus2"
    case Plus4 => "plus4"
  }
}

case class Number(number: Int, color: CardColor) extends Card
case class Stop(color: CardColor) extends Card
case class Reverse(color: CardColor) extends Card
case class Plus2(color: CardColor) extends Card
case object ChangeColor extends Card
case object Plus4 extends Card
