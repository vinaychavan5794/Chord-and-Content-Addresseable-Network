package Sharding
import Domain.{Container, Junction}
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import akka.http.scaladsl.server.directives.DebuggingDirectives

import scala.language.postfixOps
import scala.io.StdIn
import Messages._
import Sharding.SingleNodeApp.route
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings}
import akka.http.scaladsl.marshalling.{Marshal, ToResponseMarshallable}
import akka.util.Timeout

import scala.concurrent.duration._
import io.circe._
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import akka.pattern.ask
import com.typesafe.config.ConfigFactory

import scala.concurrent.Future

object ShardingMovies extends App {
  val config = ConfigFactory.load("sharded")
  implicit val system = ActorSystem(config.getString("clustering.cluster.name"), config)

  implicit val executionContext = system.dispatcher

  implicit val timeout: Timeout = 5 seconds

  import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
  import io.circe.generic.auto._

  ClusterSharding(system).start(
    typeName = SortingDecider.name,
    entityProps = SortingDecider.props,
    settings = ClusterShardingSettings(system),
    extractShardId = SortingDecider.extractShardId,
    extractEntityId = SortingDecider.extractEntityId
  )
  val decider:ActorRef = ClusterSharding(system).shardRegion(SortingDecider.name)

  val route =
    path("ping") {
      get {
        complete(HttpEntity(ContentTypes.`application/json`, """ {"status" : "ok"} """))
      }
    } ~
      path("junctions" / IntNumber / "decisionForContainer" / IntNumber){ (junctionId, containerId) =>
        get {
          val junction = Junction(junctionId)
          val container = Container(containerId)
          //        val decision = Decisions.whereShouldContainerGo(junction, container)
          //        val go = Go(decision)
          val goRes:Future[Go] = (decider ? WhereShouldIGo(junction, container)).mapTo[Go]
          complete(goRes)
        }
      }

  val port = config.getInt("application.exposed-port")
  val clientRouteLogged = DebuggingDirectives.logRequestResult("Client ReST", Logging.InfoLevel)(route)
  val bindingFuture = Http().newServerAt( "localhost", port).bind(route)

  println(s"Server online at http://localhost:8090/\nPress RETURN to stop...")
  StdIn.readLine() // let it run until user presses return
  bindingFuture
    .flatMap(_.unbind()) // trigger unbinding from the port
    .onComplete(_ => system.terminate()) // and shutdown when done
}
