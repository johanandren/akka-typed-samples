scalaVersion := "2.12.6"

val akkaVersion = "2.5.16"
libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion
)
