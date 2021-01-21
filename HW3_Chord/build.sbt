

name := "akka-sample-cluster-kubernetes"

scalaVersion := "2.13.0"
scalaVersion := "2.13.0"
lazy val akkaHttpVersion = "10.2.1"
lazy val akkaVersion = "2.6.10"
lazy val akkaManagementVersion = "1.0.9"

//assemblyMergeStrategy in assembly := {
//  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
//  case x => MergeStrategy.first
//}

libraryDependencies ++= {
  Seq(
    "ch.qos.logback" % "logback-classic" % "1.2.3",
    "com.lightbend.akka.discovery" %% "akka-discovery-kubernetes-api" % akkaManagementVersion,
    "com.lightbend.akka.management" %% "akka-management-cluster-bootstrap" % akkaManagementVersion,
    "com.lightbend.akka.management" %% "akka-management-cluster-http" % akkaManagementVersion,
    "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
    "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
    "com.typesafe.akka" %% "akka-cluster-typed" % akkaVersion,
    "com.typesafe.akka" %% "akka-cluster-sharding-typed" % akkaVersion,
    "com.typesafe.akka" %% "akka-stream-typed" % akkaVersion,
    "com.typesafe.akka" %% "akka-discovery" % akkaVersion,
    "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion % Test,

    "org.scalactic" %% "scalactic" % "3.0.8",

    "org.ddahl" %% "rscala" % "3.2.19",

    "org.scalatest" %% "scalatest" % "3.0.8" % "test",
    "com.typesafe.akka" %% "akka-remote" % "2.6.10",

    "com.typesafe.akka" %% "akka-http" % "10.1.11",

    "com.typesafe.akka" %% "akka-stream" % "2.6.10",

    "com.typesafe.akka" %% "akka-stream-testkit" % "2.6.10",

    "com.typesafe.akka" %% "akka-http-testkit" % "10.1.11")
}

mainClass in(Compile, run) := Some("Driver")
//mainClass in assembly := Some("Driver")
//assemblyJarName in assembly := "varunya_yanamadala_hw2.jar"


