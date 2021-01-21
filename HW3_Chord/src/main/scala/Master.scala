import Driver.{MAX_SERVERS, logger, timeout}
import ServerActor.{addMeToRing, getServerSnapshot, getSuccesor, loadMovie, lookUpKey, serverHashesDone}
import akka.actor.{Actor, ActorSystem, Props}
import Master.{addnode, getData, getSnapShot, loadData, lookupMovie}
import akka.pattern.ask
import akka.util.Timeout

import scala.collection.mutable
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps
import scala.util.Random

class Master(userActorId:Int, actorSystem:ActorSystem) extends Actor{
  var serverCount = Driver.MAX_SERVERS;
  val serverActorSupervisor = actorSystem.actorSelection( "akka://Actors/user/server-actor-supervisor")
  implicit val timeout = Timeout(10 seconds)
  val logger = Driver.logger

  override def receive: Receive = {
    case loadData(movie:Data) => {
      val serverActorSupervisor = actorSystem.actorSelection( "akka://Actors/user/server-actor-supervisor")
      val moviehash = Utils.hash(movie.movie_name,serverCount)
      logger.info("loading movie "+movie.movie_name+" with hash "+moviehash)
      val future = serverActorSupervisor ? loadMovie(new Data(moviehash.toInt,movie.movie_name))
      val result = Await.result(future, timeout.duration)
      sender() ! result
    }

    case lookupMovie(movie:Data) =>{
      val moviehash = Utils.hash(movie.movie_name,serverCount)
      logger.info("looking for movie "+moviehash)
      val future = serverActorSupervisor ? lookUpKey(new Data(moviehash.toInt,movie.movie_name))
      val result = Await.result(future, timeout.duration)
      sender() ! result
    }

    case addnode() => {
     // logger.info("Master - addServer ")
     var newServerHashedValue = Random.between(1, math.pow(2, MAX_SERVERS)).toInt
      while (serverHashesDone.contains(newServerHashedValue)){
        newServerHashedValue = Random.between(1,math.pow(2,15)).toInt
      }
      serverHashesDone.add(newServerHashedValue)
      ServerActor.serverCount+=1
      val newServerActor = actorSystem.actorOf(Props(new
          ServerActor(ServerActor.serverCount,newServerHashedValue,actorSystem)),"server-actor-" + ServerActor.serverCount)
      logger.info("Master - adding new server "+ServerActor.serverCount+" with hash key "+newServerHashedValue+" at "+newServerActor.path.toString)
      val future = newServerActor ? addMeToRing(newServerActor)
      val result = Await.result(future, timeout.duration)
      sender() ! result
    }

    case getSnapShot() => {
      val serversHash =  ServerActor.getServersHashedSet
      val serversPath = ServerActor.serverHashToPathMap
      val movies = ServerActor.movies
      var snapshot = "";
      snapshot+="order of servers in chord with their hashkeys and list of movies it's holding\n\n"
      serversHash.foreach(server => {
        val actor = actorSystem.actorSelection(serversPath.get(server).getOrElse(""))
        val actorPath = actor.pathString.substring(actor.pathString.lastIndexOf('/'))
        var movieList = ""

        movies.get(server).getOrElse(new mutable.TreeSet[Data]()(Data)).foreach(movie=>{
          movieList += movie.movie_name+", "
        })
        snapshot+= " actor "+actorPath+" hashkey "+server+" movies "+movieList+"\n"

      })

      logger.info("**************** SNAPSHOT ***************")
      logger.info(snapshot)

    }


  }
}

object Master {
  case class getData(movie:Data)

  case class loadData( movie:Data)

  case class addnode()

  case class getSnapShot()

  case class lookupMovie(movie:Data)
  val movieData:mutable.TreeSet[Data] = new mutable.TreeSet[Data]()(Data)
}
