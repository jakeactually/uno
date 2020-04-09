import akka.actor.ActorRef

import scala.collection.mutable

class Notifier {
  val state: mutable.Map[Int, Seq[ActorRef]] = mutable.Map()

  def notify(id: Int, message: String): Unit = {
    state.get(id).foreach(_.foreach(_ ! message))
  }
}
