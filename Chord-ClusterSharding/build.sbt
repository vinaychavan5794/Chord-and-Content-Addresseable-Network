name := "overlaynetworks_group3"

scalaVersion := "2.13.0"
lazy val akkaHttpVersion = "10.2.1"
lazy val akkaVersion = "2.6.10"
lazy val akkaManagementVersion = "1.0.9"

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case "reference.conf" => MergeStrategy.concat
  case x => MergeStrategy.first
}

libraryDependencies ++= {
  Seq(
    "ch.qos.logback" % "logback-classic" % "1.2.3",
    "com.lightbend.akka.discovery" %% "akka-discovery-kubernetes-api" % akkaManagementVersion,
    "com.lightbend.akka.management" %% "akka-management-cluster-bootstrap" % akkaManagementVersion,
    "com.lightbend.akka.management" %% "akka-management-cluster-http" % akkaManagementVersion,
    "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
    "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
    "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % "Test",
    "com.typesafe.akka" %% "akka-cluster-typed" % akkaVersion,
    "com.typesafe.akka" %% "akka-cluster-sharding-typed" % akkaVersion,
    "com.typesafe.akka" %% "akka-stream-typed" % akkaVersion,
    "com.typesafe.akka" %% "akka-discovery" % akkaVersion,
    "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion % Test,

    "org.ddahl" %% "rscala" % "3.2.19",
    "org.scalactic" %% "scalactic" % "3.0.8",

    "org.ddahl" %% "rscala" % "3.2.19",

    "org.scalatest" %% "scalatest" % "3.0.8" % "test",
    "com.typesafe.akka" %% "akka-remote" % "2.6.10",

    "com.typesafe.akka" %% "akka-http" % "10.1.11",

    "com.typesafe.akka" %% "akka-stream" % "2.6.10",

    "com.typesafe.akka" %% "akka-stream-testkit" % "2.6.10",

    "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion)
}
val circeVersion = "0.12.3"

libraryDependencies ++= Seq(
"io.circe" %% "circe-core",
"io.circe" %% "circe-generic",
"io.circe" %% "circe-parser",
).map(_ % circeVersion)

libraryDependencies ++= Seq(
"de.heikoseeberger" %% "akka-http-circe" % "1.31.0",
"io.netty" % "netty" % "3.10.6.Final"
)
mainClass in(Compile, run) := Some("Driver")
mainClass in assembly := Some("Driver")

assemblyJarName in assembly := "overlaynetwork_group3.jar"