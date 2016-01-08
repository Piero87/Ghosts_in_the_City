name := """treasures_hunters"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.6"

libraryDependencies ++= Seq(
  cache,
  ws,
  specs2 % Test,
  "org.webjars" % "jquery" % "2.1.4",
  "org.webjars" % "bootstrap" % "3.0.0",
  "org.webjars" % "knockout" % "3.4.0",
  "org.webjars" % "requirejs" % "2.1.22",
  "org.webjars" % "leaflet" % "0.7.5",
  "com.typesafe.akka" %% "akka-actor" % "2.4.1",
  "com.typesafe.akka" %% "akka-cluster" % "2.4.1",
  "com.typesafe.akka" %% "akka-cluster-tools" % "2.4.1",
  "com.typesafe.akka" %% "akka-slf4j" % "2.4.0"
)

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"

// Play provides two styles of routers, one expects its actions to be injected, the
// other, legacy style, accesses its actions statically.
routesGenerator := InjectedRoutesGenerator

// Compile the project before generating Eclipse files, so that generated .scala or .class files for views and routes are present
EclipseKeys.preTasks := Seq(compile in Compile)
