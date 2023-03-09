// build.sbt

// https://github.com/scalafx/scalafx

// https://github.com/scalafx/scalafx#demo-projects-and-examples

// Sonatype Snapshots resolver
resolvers ++= Resolver.sonatypeOssRepos("snapshots")

// https://mvnrepository.com/artifact/org.scalafx/scalafx
// https://repo1.maven.org/maven2/org/scalafx/scalafx_3/
val scalafxVersion = "18.0.2-R29"

// https://mvnrepository.com/artifact/com.typesafe.akka/akka-actor
val akkaactorVersion = "2.6.19"

// https://mvnrepository.com/artifact/com.typesafe.akka/akka-stream
val akkastreamVersion = "2.6.19"

// https://mvnrepository.com/artifact/com.typesafe/config
val configVersion = "1.4.2"

// https://mvnrepository.com/artifact/com.typesafe.scala-logging/scala-logging
val scalaloggingVersion = "3.9.5"

// https://mvnrepository.com/artifact/org.scala-lang.modules/scala-xml
val scalaxmlVersion = "2.0.1"

// https://mvnrepository.com/artifact/com.github.pathikrit/better-files
val betterfilesVersion = "3.9.1"

// https://mvnrepository.com/artifact/ch.qos.logback/logback-classic
val logbackclassicVersion = "1.3.0-beta0"

// https://mvnrepository.com/artifact/com.typesafe.akka/akka-slf4j
val akkaslf4jVersion = "2.6.19"

// https://mvnrepository.com/artifact/org.slf4j/slf4j-api
val slf4japiVersion = "2.0.0-beta1"

// https://mvnrepository.com/artifact/com.lihaoyi/sourcecode
val sourcecodeVersion = "0.3.0"

// https://mvnrepository.com/artifact/org.scalaj/scalaj-http
val scalajhttpVersion = "2.4.2"

// https://mvnrepository.com/artifact/org.jsoup/jsoup
val jsoupVersion = "1.15.2"


lazy val scalafx = "org.scalafx" %% "scalafx" % scalafxVersion
lazy val akkaactor = "com.typesafe.akka" %% "akka-actor" % akkaactorVersion
lazy val akkastream = "com.typesafe.akka" %% "akka-stream" % akkastreamVersion
lazy val config = "com.typesafe" % "config" % configVersion
//lazy val scalalogging = "com.typesafe.scala-logging" %% "scala-logging" % scalaloggingVersion
lazy val scalaxml = "org.scala-lang.modules" %% "scala-xml" % scalaxmlVersion
lazy val betterfiles = "com.github.pathikrit" %% "better-files" % betterfilesVersion
//lazy val logbackclassic = "ch.qos.logback" % "logback-classic" % logbackclassicVersion
lazy val akkaslf4j = "com.typesafe.akka" %% "akka-slf4j" % akkaslf4jVersion
//lazy val slf4japi = "org.slf4j" % "slf4j-api" % slf4japiVersion
lazy val sourcecode = "com.lihaoyi" %% "sourcecode" % sourcecodeVersion
lazy val scalajhttp = "org.scalaj" %% "scalaj-http" % scalajhttpVersion
lazy val jsoup = "org.jsoup" % "jsoup" % jsoupVersion


inThisBuild(
  List(
    //scalaVersion := sys.env.get("DOTTY_LATEST").getOrElse("3.1.3"),
    scalaVersion := "2.13.8",
    fork := true
  )
)

lazy val root = (project in file("."))
  .settings(
    Compile / mainClass := Some("de.jvr.pricecompare.Pricecompare"),
    organization := "de.jvr",
    name := "pricecompare",
    libraryDependencies ++= Seq(
      scalafx,
      akkaactor,
      akkastream,
      config,
      //scalalogging,
      scalaxml,
      betterfiles,
      //logbackclassic,
      //slf4japi,
      sourcecode,
      scalajhttp,
      jsoup,
      ),
    scalacOptions ++= Seq(
      "-feature",
      "-deprecation",
      "-unchecked",
      "-language:postfixOps"
    ),
    assembly / assemblyMergeStrategy := {
     case PathList("META-INF", xs @ _*) => MergeStrategy.discard
     case x => MergeStrategy.first
    },
    assembly / assemblyJarName := "pricecompare.jar",
      javaOptions += "-Dconfig.override_with_env_vars=true",
    javaOptions += "-D--module-path=/org/openjfx/",
    javaOptions += "-D--add-modules=javafx.controls,javafx.fxml,javafx.graphics,javafx.media,javafx.swing,javafx.web",
    javaOptions += "-D--add-reads=javafx.graphics=ALL-UNNAMED",
    javaOptions += "-D--add-opens=javafx.graphics/com.sun.glass.ui=ALL-UNNAMED",
    javaOptions += "-D--add-opens=javafx.controls/com.sun.javafx.charts=ALL-UNNAMED",
    javaOptions += "-D--add-opens=javafx.graphics/com.sun.javafx.iio=ALL-UNNAMED",
    javaOptions += "-D--add-opens=javafx.graphics/com.sun.javafx.iio.common=ALL-UNNAMED",
    javaOptions += "-D--add-opens=javafx.graphics/com.sun.javafx.css=ALL-UNNAMED",
    javaOptions += "-D--add-opens=javafx.graphics/com.sun.javafx.application=ALL-UNNAMED",
    javaOptions += "-D--add-opens=javafx.base/com.sun.javafx.runtime=ALL-UNNAMED",
  )
  
  
// creates _CopyDependencyClasspath.bat:
val create_bat_DependencyClasspath = taskKey[Unit]("Creates _CopyDependencyClasspath.bat and _updaterfiles$_$_$_append.txt")

create_bat_DependencyClasspath := {
  import scala.tools.nsc.io.File

  val quot = "\""
  val cp = (Compile / dependencyClasspath).value
  //cp.foreach(f => println(s"${f.metadata.get(moduleID.key)} => ${f.data}"))
  cp.foreach(f => {
    val pn = (f.data).toString
    val fn = (pn.split("\\\\").takeRight(1))(0)
    val pnDecoded = java.net.URLDecoder.decode(pn, java.nio.charset.StandardCharsets.UTF_8.toString())
    val fnDecoded = java.net.URLDecoder.decode(fn, java.nio.charset.StandardCharsets.UTF_8.toString())
    
    File("_CopyDependencyClasspath.bat").appendAll("copy /Y " + quot + pn + quot + " "  + quot + """.\lib\""" + fnDecoded  + quot + "\n")
    File("_updaterfiles$_$_$_append.txt").appendAll(fnDecoded + ",,,lib" + "\n")
  })
}

val showDependencies = taskKey[Unit]("Prints file-names of the dependencies")

showDependencies := {
  import scala.tools.nsc.io.File

  val quot = "\""
  val cp = (Compile / dependencyClasspath).value
  //cp.foreach(f => println(s"${f.metadata.get(moduleID.key)} => ${f.data}"))
  cp.foreach(f => {
    val pn = (f.data).toString
    val fn = (pn.split("\\\\").takeRight(1))(0)
    val pnDecoded = java.net.URLDecoder.decode(pn, java.nio.charset.StandardCharsets.UTF_8.toString())
    val fnDecoded = java.net.URLDecoder.decode(fn, java.nio.charset.StandardCharsets.UTF_8.toString())
    
    println(fnDecoded)
  })
}





