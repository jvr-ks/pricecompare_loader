ThisBuild / organization := "de.jvr"
ThisBuild / scalaVersion := "2.13.3"
ThisBuild / version      := "1.0"

lazy val javaFXModules = Seq("base", "controls", "graphics", "media", "web")

lazy val root = (project in file("."))
.settings(
	name := "Pricecompare",
	logLevel := Level.Warn,
	Compile / mainClass := Some("de.jvr.pricecompare.Pricecompare"),
	fork := true,
	Compile / packageBin / artifactPath := baseDirectory.value / "pricecompare.jar",
	scalacOptions ++= Seq("-deprecation", "-feature", "-language:postfixOps","-Ywarn-unused","-Yrangepos"),
	
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
	
	libraryDependencies ++= Seq(
		"org.scalafx" 								%% "scalafx" 						% "16.0.0-R25",
		"com.typesafe.akka"						%% "akka-actor"         % "latest.integration",
		"com.typesafe.akka"						%% "akka-stream"				% "latest.integration",
		"com.typesafe" 								% "config" 							% "1.4.1",
		"com.typesafe.scala-logging"	%% "scala-logging"			% "latest.integration",
		"org.scala-lang.modules" 			%% "scala-xml" 					% "2.0.1",
		"com.github.pathikrit"				%% "better-files"				% "latest.integration",
		"ch.qos.logback" 							% "logback-classic" 		% "1.2.9",
		"org.slf4j"										% "slf4j-api"						% "1.7.30",
		"com.lihaoyi" 								%% "sourcecode" 				% "0.1.9",
		"org.scalaj"									%% "scalaj-http"				% "2.4.2",
	),
	
	// Add JavaFX dependencies
	libraryDependencies ++= {
		// Determine OS version of JavaFX binaries
		lazy val osName = System.getProperty("os.name") match {
			case n if n.startsWith("Linux") => "linux"
			case n if n.startsWith("Mac") => "mac"
			case n if n.startsWith("Windows") => "win"
			case _ => throw new Exception("Unknown platform!")
		}
		Seq("base", "controls", "fxml", "graphics", "media", "swing", "web")
			.map(m => "org.openjfx" % s"javafx-$m" % "16-ea+7" classifier osName)
	}
)

val printDependencyClasspath = taskKey[Unit]("Prints location of the dependencies")

printDependencyClasspath := {
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
    File("updaterfiles$_$_$_append.txt").appendAll(fnDecoded + ",,,lib" + "\n")
  })
}



