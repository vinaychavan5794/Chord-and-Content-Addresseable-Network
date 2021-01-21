package Sharding

import Domain.{Container, Junction}
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives.DebuggingDirectives
import scala.language.postfixOps
import scala.io.StdIn
import Messages._
import akka.http.scaladsl.marshalling.{Marshal, ToResponseMarshallable}
import akka.util.Timeout

import scala.concurrent.duration._
import io.circe._
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._

import akka.pattern.ask

import scala.concurrent.Future


object SingleNodeApp extends App {

  implicit val system = ActorSystem("singleActorSystem")

  implicit val executionContext = system.dispatcher

  implicit val timeout: Timeout = 5 seconds

  import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
  import io.circe.generic.auto._

  val decider:ActorRef = system.actorOf(Props[DecidersGuardian])

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

  val clientRouteLogged = DebuggingDirectives.logRequestResult("Client ReST", Logging.InfoLevel)(route)
  val bindingFuture = Http().newServerAt( "localhost", 8090).bind(route)
  println(s"Server online at http://localhost:8090/\nPress RETURN to stop...")
  StdIn.readLine() // let it run until user presses return
  bindingFuture
    .flatMap(_.unbind()) // trigger unbinding from the port
    .onComplete(_ => system.terminate()) // and shutdown when done
}
