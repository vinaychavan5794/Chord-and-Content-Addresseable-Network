import Driver.{actorSystem, logger, movies, timeout}
import Master.{loadData, lookupMovie}
import UserActor.{addUser, loadMovie, lookMovie}
import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout
import scala.language.postfixOps
import scala.collection.mutable
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class UserActor (serverId: Int,serverHash: Int,actorSystem:ActorSystem) extends Actor{

  var MAX_SERVERS = Driver.MAX_SERVERS
  //fingerTable :(key, key's (Successor hash value, Successor actor's path))

  private var currServerHashedValue: Int = serverHash
  implicit val timeout = Timeout(6 seconds)
  private val logger = Driver.logger
  private var userCount = UserActor.userCount

  override def receive: Receive = {

    case addUser(curServer:ActorRef) =>{
      var userActorSet = UserActor.userActorHashedTreeSet.add(serverHash)
      UserActor.registerUser(serverHash,curServer.path.toString)
    }


    case lookMovie(movie:Data)=> {
      val master = actorSystem.actorSelection("akka://Actors/user/master-actor")
      val future3 = master ? lookupMovie(movie)
      val result3 = Await.result(future3, timeout.duration)
      sender() ! result3
      logger.info("result3 : " + result3.toString)
    }

    case loadMovie(movie:Data) => {
      val master = actorSystem.actorSelection("akka://Actors/user/master-actor")
      logger.info(""+master.pathString)
      val future3 = master ? loadData(movie)
      val result3 = Await.result(future3, timeout.duration)
      sender() ! result3
      logger.info("result3 : " + result3.toString)
    }


  }

}


object UserActor {

  val userActorHashedTreeSet:mutable.TreeSet[Int] = new mutable.TreeSet[Int]()
  val userHashToPathMap = new mutable.HashMap[Int, String]()
  var userCount = 0

  private val logger = Driver.logger


  //responsible for adding server node
  case class addUser(curServer:ActorRef)
  //responsible for loading key into server node.
  case class loadMovie(movie:Data)
  //responsible for looking key
  case class lookMovie(movie:Data)


  //registers server by adding it's hash and path to respective tables
  def registerUser(hashKey:Int, serverPath:String): Boolean = {
    userActorHashedTreeSet += hashKey
    userHashToPathMap += (hashKey -> serverPath)

    true
  }



}
