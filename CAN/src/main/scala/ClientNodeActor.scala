import ClientNodeActor.{addFile, addMeToCAN, lookupFile, removeMeFromCAN}
import akka.actor.{Actor, ActorSystem}

class ClientNodeActor(client: Client,actorSystem:ActorSystem) extends Actor {
  var me: Client = client
  override def receive: Receive = {
    case addMeToCAN(client: Client) => {
      me = client
      Client.idToClientMap.addOne(Client.count,client)
      client.join()
    }

    case removeMeFromCAN() => {
      me.leave()
      Client.idToClientMap.remove(me.identifier.toInt)

    }
    case addFile(file: String) => {
      me.addFile(file,me.myIPAddress)
    }
    case lookupFile(file:String) => {
      me.searchFile(file)
    }

  }
}

object ClientNodeActor {
  sealed case class addMeToCAN(client: Client)
  sealed case class removeMeFromCAN()
  sealed case class addFile(file:String)
  sealed case class lookupFile(file:String)


}
