package Sharding
import akka.actor.{Actor, ActorLogging, Props}
import akka.cluster.sharding.ShardRegion
import Domain._
import Messages._
import akka.actor.Actor.Receive
import akka.cluster.sharding.ShardRegion.{ExtractEntityId, ExtractShardId}
import akka.event.LoggingReceive

class SortingDecider extends Actor with ActorLogging {

  override def receive: Receive = LoggingReceive {
    case WhereShouldIGo(junction, container) =>
      val decision = Decisions.whereShouldContainerGo(junction, container)
      log.info("Decision on junction {} for container {}: {}", junction.id, container.id, decision)
      sender() ! Go(decision)
  }
}


object SortingDecider {

  def name = "sortingDecider"
  def props = Props[SortingDecider]

  def extractShardId:ExtractShardId = {
    case WhereShouldIGo(junction, _) => (junction.id % 2).toString
  }

  def extractEntityId:ExtractEntityId = {
    case msg @ WhereShouldIGo(junction, _) =>
      (junction.id.toString, msg)
  }
}