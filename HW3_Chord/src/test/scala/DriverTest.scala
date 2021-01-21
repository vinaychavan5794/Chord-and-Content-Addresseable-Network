
import Driver.{actorSystem, getMovies, logger, movies, timeout}
import Master.addnode
import UserActor.{loadMovie, lookMovie}
import akka.actor.{ActorSystem, Props}
import akka.pattern.ask
import org.scalatest.{BeforeAndAfter, FunSuite}

import scala.concurrent.Await

class DriverTest extends FunSuite with BeforeAndAfter  {
  val actorSystem = ActorSystem("Actors")
  val serverSupervisor = actorSystem.actorOf(Props(new ServerActor(0,0, actorSystem)), "server-actor-supervisor")

  val master = actorSystem.actorOf(Props(new Master(1, actorSystem)), "master-actor")
  val user = actorSystem.actorOf(Props(new UserActor(0,0,actorSystem)),name = "user-actor")


  private val logger = Driver.logger

  test("Driver.getMovies") {
    val resultList = Driver.getMovies()
    val verifyList = resultList.distinct
    assert(resultList.length > 0 && resultList == verifyList)
  }

  test("Utils.hash"){
    val algo = "SHA-1"
    val result = Utils.hash("id_1", 10)
    assert(result.toInt <= math.pow(2, 10))
  }

  test("Server add test"){
    val future = master ? addnode()
    val result = Await.result(future, timeout.duration)
    assert(true,result !=null)
    //actorSystem.terminate()

  }

  test("load and lookup success test"){
    val movies = Driver.getMovies()
    val future = master ? addnode()
    val result = Await.result(future, timeout.duration)

    val future4 = user ? loadMovie(movies(5))
    val result4 = Await.result(future4, timeout.duration)

    logger.info("-----------------------------------------------------------")
    val future5 = user  ? lookMovie(movies(5))
    val result5 = Await.result(future5, timeout.duration)
    assert(true,result5.toString.contains("found at server"))

    //actorSystem.terminate()
  }

  test("load and lookup fail test"){
    val movies = Driver.getMovies()
    val future = master ? addnode()
    val result = Await.result(future, timeout.duration)

    val future4 = user ? loadMovie(movies(5))
    val result4 = Await.result(future4, timeout.duration)

    logger.info("-----------------------------------------------------------")
    val future5 = user  ? lookMovie(movies(4))
    val result5 = Await.result(future5, timeout.duration)
    assert(true,result5.toString.contains("doesn't exist"))

    //actorSystem.terminate()
  }



}
