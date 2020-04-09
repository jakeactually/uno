package models

sealed class CardColor

case object Red extends CardColor
case object Green extends CardColor
case object Blue extends CardColor
case object Yellow extends CardColor

sealed class Card

case class Number(number: Int, color: CardColor) extends Card
case class Stop(color: CardColor) extends Card
case class Reverse(color: CardColor) extends Card
case class Plus2(color: CardColor) extends Card
case object ChangeColor extends Card
case object Plus4 extends Card
