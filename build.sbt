scalaVersion := "2.13.1"
javacOptions ++= Seq("-source", "10", "-target", "10")

val akkaVersion = "2.6.3"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor-typed"           % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster-typed"         % akkaVersion,
  "ch.qos.logback"    %  "logback-classic"            % "1.2.3",
  "com.typesafe.akka" %% "akka-serialization-jackson" % akkaVersion,
  // "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion % "test"
)