import java.util.logging.Logger
import Master.{addnode, getSnapShot}
import UserActor.lookMovie
import UserActor.loadMovie
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse}
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask

import scala.language.postfixOps
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.typesafe.config.ConfigFactory

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.{Await, ExecutionContextExecutor}
import scala.concurrent.duration._
import scala.io.{Source, StdIn}


object WebService {

  val MAX_SERVERS: Int = ConfigFactory.load("config.conf"  ).getConfig("config").getInt("max_servers")
  val movies: Seq[Data] = getMovies()

  def getMovies()  : List[Data] = {
    val resData: ListBuffer[Data] = new ListBuffer[Data]
    val lines = Source.fromResource("data.csv")
    var i: Int = 0
    for (line <- lines.getLines.drop(1)) {
      val cols = line.split(",")
      val data:Data = new Data(i,cols(0))
      resData += data
      i = i + 1
    }
    resData.toList
  }

  val logger:Logger = Logger.getLogger("Actors")
  val actorSystem = ActorSystem("Actors")
  implicit val timeout = Timeout(10 seconds)

  def main(args: Array[String]) {
    implicit val system: ActorSystem = ActorSystem()
    var serverNodeCreated = false
    // needed for the future flatMap/onComplete in the end
    implicit val executionContext: ExecutionContextExecutor = system.dispatcher


    logger.info("starting")
    val serverSupervisor = actorSystem.actorOf(Props(new ServerActor(0,0, actorSystem)), "server-actor-supervisor")

    val master = actorSystem.actorOf(Props(new Master(1, actorSystem)), "master-actor")
    var userCount = 0;
    var users:mutable.ListBuffer[ActorRef] = new ListBuffer[ActorRef]()

    (0 to MAX_SERVERS).foreach(i=>{
      users.addOne(actorSystem.actorOf(Props(new UserActor(userCount,userCount,actorSystem)),name = "user-actor-"+userCount))
      userCount+=1;
    })

    val route =
      get {
        concat(

          pathSingleSlash {
            //actorSystemDriver.ActorSystemDriver
            complete(HttpEntity(
              ContentTypes.`text/html(UTF-8)`,
              "<html><body> <a href=\"http://127.0.0.1:8080/addNode\">1. Add a Node</a><br> " +
                "<a href=\"http://127.0.0.1:8080/loadData\">2. Load Data to the Server</a><br> " +
                "<a href=\"http://127.0.0.1:8080/lookupData\">3. Lookup Data on a Server</a><br> " +
                "<a href=\"http://127.0.0.1:8080/getSnapshot\">4. Get System Snapshot</a><br> " +
                "</body></html>"))
          },


          //Route definition for adding a node to the server.
          path("addNode") {
            val future = master ? addnode()
            val result = Await.result(future, timeout.duration)

            if (result!=null) {
              serverNodeCreated = true
              complete(HttpResponse(entity = HttpEntity(
                ContentTypes.`text/html(UTF-8)`,
                "<html><body> Added a node! <br><a href=\"http://127.0.0.1:8080/\">Go Back</a><br><br> </body></html>")))

            }
            else
              complete(HttpResponse(entity = "Can't add more servers to the system."))
          },


          //Route definition for loading movie data to a node.
          path("loadData") {
            parameters('id) { (id) =>
              if (serverNodeCreated) {
                if (id.toInt >= movies.length) {
                  complete("Enter id between 0 and " + (movies.length - 1) + "")
                }
                else {
                  val future5 = users(1)  ? loadMovie(movies(id.toInt))
                  val result = Await.result(future5, timeout.duration)
                  complete(HttpResponse(entity = HttpEntity(
                    ContentTypes.`text/html(UTF-8)`,
                    "<html><body> Loaded Data. <br><a href=\"http://127.0.0.1:8080/\">Go Back</a><br> </body></html>")))
                }
              } else {
                complete(HttpResponse(entity = HttpEntity(
                  ContentTypes.`text/html(UTF-8)`,
                  "<html><body> Add a server node first! <br><a href=\"http://127.0.0.1:8080/\">Go Back</a><br> </body></html>")))
              }
            }
          },

          //Route definition for lookup.
          path("lookupData") {
            parameters(Symbol("id")) { (id) =>
              if (serverNodeCreated) {
                val future5 = users(2)  ? lookMovie(movies(id.toInt))
                val result = Await.result(future5, timeout.duration)
                complete(HttpResponse(entity = HttpEntity(
                  ContentTypes.`text/html(UTF-8)`,
                  "<html><body> Data(id = " + id + ") found at server path: " + result + " <br><a href=\"http://127.0.0.1:8080/\">Go Back</a><br> </body></html>")))

              } else {
                complete(HttpResponse(entity = HttpEntity(
                  ContentTypes.`text/html(UTF-8)`,
                  "<html><body> Add a server node first! <br><a href=\"http://127.0.0.1:8080/\">Go Back</a><br> </body></html>")))
              }
            }
          },

          //Route definition for creating a system snapshot
          path("getSnapshot") {

            if (serverNodeCreated) {
              val result: Unit =master ! getSnapShot()

              complete(HttpResponse(entity = HttpEntity(
                ContentTypes.`text/html(UTF-8)`,
                "<html><body> " + result + " <br><a href=\"http://127.0.0.1:8080/\">Go Back</a><br> </body></html>")))
            } else {
              complete(HttpResponse(entity = HttpEntity(
                ContentTypes.`text/html(UTF-8)`,
                "<html><body> Add a server node first! <br><a href=\"http://127.0.0.1:8080/\">Go Back</a><br> </body></html>")))
            }
          }
        )
      }

    // `route` will be implicitly converted to `Flow` using `RouteResult.route2HandlerFlow`
    val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)
    println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
    StdIn.readLine() // let it run until user presses return
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => system.terminate()) // and shutdown when done
  }
}
