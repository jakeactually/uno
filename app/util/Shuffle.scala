package util

import scala.util.Random

object Shuffle {
  def shuffle[T](array: Array[T]): Array[T] = {
    for (_ <- 0 to array.length) {
      val a = Random.nextInt(array.length)
      val b = Random.nextInt(array.length)
      val temp = array(a)
      array(a) = array(b)
      array(b) = temp
    }

    array
  }
}
