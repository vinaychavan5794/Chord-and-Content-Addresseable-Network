import java.util.logging.Logger

import ClientNodeActor.{addFile, addMeToCAN, lookupFile, removeMeFromCAN}
import akka.actor.{ActorSystem, Props}

object Driver {

  val logger:Logger = Logger.getLogger("log")
  val actorSystem = ActorSystem("actorSystem")

  def main(args: Array[String]): Unit = {
    var count = 0;

    val client = new Client(count)
    val newClientActor = actorSystem.actorOf(Props(new
        ClientNodeActor(client,actorSystem)),"client-actor-" + Client.count)
    newClientActor ! addMeToCAN(client)
    newClientActor ! addFile("Test")
    newClientActor ! lookupFile("Test")




  }

}
