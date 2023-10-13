/************************************************
* pricecompare.scala
*
* License GNU GENERAL PUBLIC LICENSE see License.txt
************************************************/
// ü

package de.jvr.pricecompare


import scala.concurrent._
import scala.concurrent.duration._
import scala.xml.XML

import scala.util.Try
import scala.util.control.NonFatal

import akka.actor.{Props, ActorSystem}
import akka.util.Timeout

import scalafx.Includes._
import scalafx.application.JFXApp3
import scalafx.application.JFXApp3.PrimaryStage
import scalafx.collections.ObservableBuffer

import scalafx.stage.FileChooser
import scalafx.stage.FileChooser.ExtensionFilter

import scalafx.stage.Stage
import scalafx.scene.Scene
 
import scalafx.scene.control._
import scalafx.scene.control.Alert.AlertType
import scalafx.scene.layout._
import scalafx.scene.layout.GridPane

import scalafx.scene.control.TableColumn._
import scalafx.scene.control.TableView

import scalafx.scene.paint.Color
import scalafx.scene.shape.Circle

import scalafx.scene.control.Menu

import scalafx.scene.image.Image
import scalafx.beans.property.StringProperty

import scalafx.geometry.Insets

import javafx.application.Platform._

import com.typesafe.config.ConfigFactory

import better.files._
import better.files.Dsl._
import java.io.{File => JFile}

import sys.process._

import de.jvr.pricecompare.GuiUpdateActor
import de.jvr.pricecompare.GuiUpdateActor._

//---------------------------------- Urlfile ----------------------------------
object Urlfile {
  val configfile = ("""application.conf""").replace("\\","/")
  val pricecompareconfig = ConfigFactory.parseFile(new java.io.File(configfile))
  
  var theUrlFile = StringProperty("pricecompare_urls.txt")
  var running = false
  var forcefastmode = false
  var fastmode = false
  var timeout = 60000
  
  def configRefresh() = {
    if (pricecompareconfig.hasPath("test")){
      if (pricecompareconfig.getString("test").toLowerCase.contains("yes")){
        theUrlFile() = "pricecompare_urls_test.txt"
        Pricecompare.showAlertInfo(s"urlfile: ${theUrlFile()}\n(from application.conf file)")
      }
    }
    
    if (pricecompareconfig.hasPath("forcefastmode")){
      if (pricecompareconfig.getString("forcefastmode").toLowerCase.contains("yes")){
        fastmode = true
      }
    }
    
    if (pricecompareconfig.hasPath("timeout")){
      timeout = pricecompareconfig.getString("timeout").toInt
    }
  }
  
  configRefresh()
}

//---------------------------------- Guidata ----------------------------------
object Guidata {
  val configfile = ("""application.conf""").replace("\\","/")
  val pricecompareconfig = ConfigFactory.parseFile(new java.io.File(configfile))
  
  var autostart = false
  var autoclose = false
  var sound = true
  var customcss = false
  var viewlines = 10
  
  if (pricecompareconfig.hasPath("autostart")){
    if (pricecompareconfig.getString("autostart").toLowerCase.contains("yes")){
      autostart = true
    }
  }
      
  if (pricecompareconfig.hasPath("nosound")){
    if (pricecompareconfig.getString("nosound").toLowerCase.contains("yes")){
      sound = false
    }
  }
  
  if (pricecompareconfig.hasPath("customcss")){
    if (pricecompareconfig.getString("customcss").toLowerCase.contains("yes")){
      customcss = true
    }
  }
  // autoclose not used!
  if (pricecompareconfig.hasPath("autoclose")){
    if (pricecompareconfig.getString("autoclose").toLowerCase.contains("yes")){
      autoclose = true
    }
  }
  
  if (pricecompareconfig.hasPath("viewlines")){
    viewlines = pricecompareconfig.getString("viewlines").toInt
  }
  
  if (pricecompareconfig.hasPath("modena")){
    if (pricecompareconfig.getString("modena").toLowerCase.contains("yes")){
      System.setProperty( "javafx.userAgentStylesheetUrl", "MODENA" )
    }
  }
  
  var framePosX = cnf.leftDefault
  var framePosY = cnf.topDefault
  var frameWidth = cnf.widthDefault
  var frameHeight = cnf.heightDefault
  
  var taHeight = cnf.heightDefault * 0.4
  
  var rowsMAX = 1
}
//------------------------------- Pricecompare -------------------------------
object Pricecompare extends JFXApp3 {

  val version = "0.145"
  val appVersion = "0.134"

  val appname = "pricecompare"
  val appnameUpper = "Pricecompare"
  
  val frameBorderX = 5

  val nl = System.getProperty( "line.separator" )
  // val fsepa = java.io.File.separator
  
  val wrkDir = """C:\___jvr_work\___workspaces\____scala\pricecompare\src\main\scala\""" // shorten displayed lines " np2 correct
  
  //val wrkDir = System.getProperty("user.dir") + JFile.separator
  
  val versionsurl = "https://github.com/jvr-ks/pricecompare/raw/main/version.html"
  
  val updateUrl = "https://github.com/jvr-ks/pricecompare"
  
  val help_online_url = "http://www.jvr.de/" + appname
  
  val extractorFileName = "priceextractors.txt"
  val alertSoundFile = "alertsound.mp3"
  val guiconfigFile = "guiconfig.xml"
    
  System.setProperty("file.encoding", "UTF-8")
  
  //System.setProperty( "javafx.userAgentStylesheetUrl", "CASPIAN" )
  

  //-------------------------------- ActorSystem --------------------------------
  implicit val system = ActorSystem("ActorSystem")
  implicit val executionContext = system.dispatcher
  implicit val timeout = Timeout(5 second)
  
  val pricecompareWorkerActor = system.actorOf(Props[WorkerActor](), "pricecompareWorkerActor")
  val pricecompareGuiUpdateActor = system.actorOf(Props[GuiUpdateActor](), "pricecompareGuiUpdateActor")
  val pricecompareReadUrlActor = system.actorOf(Props[ReadUrlActor](), "pricecompareReadUrlActor")
  
  
  // scalafx.collections.ObservableBuffer
  val rowsBuffer = ObservableBuffer[PricecompareResultRow]()

  // TODO:
  var pricecompareResultView: TableView[PricecompareResultRow] = _
  var ta: TextArea = _
  
  //---------------------------------- openURL ----------------------------------
  def openURL(url: String) = {
    try {
      val desktop = java.awt.Desktop.getDesktop()
      desktop.browse(new java.net.URI( url ))
    } catch {
      case e:Throwable => {
          pricecompareGuiUpdateActor ! GuiUpdateActor.Ta_add(s"\nError opening browser: $e\n")
        }
    }
    ""
  }
  
  //------------------------------- openFileAsURL -------------------------------
  def openFileAsURL(url: String) = {
    try {
      var openURL = url
      if (url.contains(":")){
        openURL = """file:///""" + url.replace("\\","/")
      }
      val desktop = java.awt.Desktop.getDesktop()
      desktop.browse(new java.net.URI( openURL ))
    } catch {
      case e:Throwable => {
          pricecompareGuiUpdateActor ! GuiUpdateActor.Ta_add(s"\nError opening browser: $e\n")
        }
    }
    ""
  }
  //---------------------------------- stopAll ----------------------------------
  def stopAll(): Unit = {
    //#Shutdown
    stage.title = "Shutdown ... bye ..."

    pricecompareReadUrlActor ! akka.actor.PoisonPill
    pricecompareGuiUpdateActor ! akka.actor.PoisonPill
    Await.result(system.terminate(), 20.seconds)
    stage.close()
  }
  
//----------------------------------- start -----------------------------------
  override def start(): Unit = {
    
    if (!((parameters.named).isEmpty)){
      if (parameters.named.contains("urlfile")) {
        Urlfile.theUrlFile() = parameters.named("urlfile")
      }
      
      if (parameters.named.contains("fastmode")) {
        if ((parameters.named("fastmode").toLowerCase()).contains("yes")){
          Urlfile.fastmode = true
        }
      }
    }
    //------------------------------------ gui ------------------------------------
    
    //------------------------------------ ta ------------------------------------
    ta = new TextArea {
      prefHeight = Guidata.taHeight
      hgrow = Priority.Always
      vgrow = Priority.Always
      padding = Insets(5, 5, 5, 5)
      text = ""
    }
    
    //-------------------------- pricecompareResultView --------------------------
    pricecompareResultView = new TableView[PricecompareResultRow](rowsBuffer) {
      columns ++= List(
        new scalafx.scene.control.TableColumn[PricecompareResultRow, String] {
          text = "No."
          cellValueFactory = {_.value.index}
          prefWidth = 30
        },
        new scalafx.scene.control.TableColumn[PricecompareResultRow, String]() {
          text = "Url"
          cellValueFactory = {_.value.url}
          prefWidth = 550
        },
        new scalafx.scene.control.TableColumn[PricecompareResultRow, String] {
          text = "List"
          cellValueFactory = {_.value.priceList}
          prefWidth = 60
        },
        new scalafx.scene.control.TableColumn[PricecompareResultRow, String] {
          text = "Web"
          cellValueFactory = {_.value.priceWeb}
          prefWidth = 100
        },
        new scalafx.scene.control.TableColumn[PricecompareResultRow, Color] {
          text = "Ok"
          
          cellValueFactory = _.value.okColor
          cellFactory = (cell,color) => {
            cell.graphic = Circle(fill = color, radius = 8)
          }
        }
      )
      padding = Insets(5, 5, 5, 5)
    }
    
    //-------------------------------- menuResults --------------------------------
    def menuResults = new Menu( "Results" ){
      items = Seq(
        new MenuItem {
          text = "Pricechanged"
          onAction = (_) => {
            openFileAsURL( "pricechanged.html" )
          }
        },
        new MenuItem {
          text = "Notfound"
          onAction = (_) => {
            openFileAsURL( "notfound.html" )
          }
        }
      )
    }
    
    //-------------------------------- menuUpdate --------------------------------
    def menuUpdate = new Menu( "Update" ){
      items = Seq(
        new MenuItem {
          text = "Check for updates"
          onAction = (_) => {
            pricecompareGuiUpdateActor ! GuiUpdateActor.CHECKVERSION
          }
        },
        new MenuItem {
          text = "Download updates"
          onAction = (_) => {
          //#Shutdown
          pricecompareReadUrlActor ! akka.actor.PoisonPill
          pricecompareGuiUpdateActor ! akka.actor.PoisonPill
          
          val wd = pwd
          Seq("cmd.exe", "/C", "updater.exe runMode --usePath=" + wd.toString).!
          
          Await.result(system.terminate(), 20.seconds)
          stage.close()
          }
        }
      )
    }
    
    //--------------------------------- menuHelp ---------------------------------
    def menuHelp = new Menu( "Help" ){
      items = Seq(
        new MenuItem {
          text = "Online-Help"
          onAction = (_) => {
            openURL( help_online_url )
          }
        },
        
        // new MenuItem {
          // text = "Help"
          // onAction = (_) => {
            // openFileAsURL( "README.html" )
          // }
        // }
      )
    }
    
    //-------------------------------- menuLicense --------------------------------
    def menuLicense = new Menu( "License" ){
      items = Seq(
        new MenuItem {
          text = "Show License regarding audiofile"
          onAction = (_) => {
            pricecompareGuiUpdateActor ! GuiUpdateActor.Ta_replace(Licenses.audio1)
            pricecompareGuiUpdateActor ! GuiUpdateActor.TA_SCROLLTOTOP
        }},
        
        new MenuItem {
          text = "Show License regarding app"
          onAction = (_) => {
            pricecompareGuiUpdateActor ! GuiUpdateActor.Ta_replace(Licenses.gnu)
            pricecompareGuiUpdateActor ! GuiUpdateActor.TA_SCROLLTOTOP
          }
        }
      )
    }
    
    //--------------------------------- menuQuit ---------------------------------
    def menuQuit = new Menu( "Quit" ){
      items = Seq(
        new MenuItem {
          text = "Exit app"
          onAction = (_) => {
            stopAll()
          }
        }
      )
    }
    
    //-------------------------------- menuCompare --------------------------------
    def menuCompare = new Menu( "Compare prices" ){
      items = Seq(
        new MenuItem {
          text = "Start"
          onAction = (_) => {
            // TODO inhibit if already running
            Urlfile.configRefresh()
            Urlfile.running = true
            compare(Urlfile.theUrlFile())
          }
        },
        new MenuItem {
          text = "Start fastmode"
          onAction = (_) => {
            // TODO inhibit if already running
            Urlfile.configRefresh()
            Urlfile.fastmode = true
            Urlfile.running = true
            compare(Urlfile.theUrlFile())
          }
        },
        new MenuItem {
          text = "Select UrlFile"
          onAction = (_) => {
            val fchose = new FileChooser()
            fchose.setTitle("Open Resource File")
            fchose.getExtensionFilters().addAll(new ExtensionFilter("Text Files", "*.txt"))
            fchose.setInitialDirectory(new JFile(new java.io.File(".").getAbsolutePath))
            val selectedFile = fchose.showOpenDialog(new Stage())
            if (selectedFile != null) {
              Urlfile.theUrlFile() = selectedFile.toString
              pricecompareGuiUpdateActor ! GuiUpdateActor.Ta_addCLEAR(s"Using UrlFile: ${Urlfile.theUrlFile()}\n")
            }
          }
        },
        new MenuItem {
          text = "Show UrlFile"
          onAction = (_) => {
            openFileAsURL( Urlfile.theUrlFile() )
          }
        }
      )
    }
    
    //---------------------------------- menuBar ----------------------------------
    val menuBar = new scalafx.scene.control.MenuBar {
      useSystemMenuBar = true
      minWidth = Guidata.frameWidth
    }
    
    val menus = Array(menuCompare, menuResults, menuUpdate, menuHelp, menuLicense, menuQuit)
    for (i <- menus) menuBar.menus.add( i )
    
    //----------------------------------- stage -----------------------------------
    stage = new PrimaryStage {
      title = "Pricecompare Version: " + version
      width = Guidata.frameWidth
      height = Guidata.frameHeight
      x = Guidata.framePosX
      y = Guidata.framePosY
      
      val gp1 = new GridPane() {
        hgap = 4
        vgap = 6
        margin = Insets(18)
        add(menuBar, 0, 0)
        add(pricecompareResultView, 0, 1)
        add(ta, 0, 2)

        prefWidth = 497.0
        prefHeight = 445.0
      }
      
      //----------------------------------- scene -----------------------------------
      scene = new Scene() {
        root = gp1
        if (Guidata.customcss) stylesheets = List(getClass.getClassLoader().getResource("pricecompare.css").toExternalForm)
      }
    }

    stage.widthProperty().addListener(_ -> {
      pricecompareGuiUpdateActor ! GuiUpdateActor.SAVEGUIPARAM
    });
    
    stage.heightProperty().addListener(_ -> {
      pricecompareGuiUpdateActor ! GuiUpdateActor.SAVEGUIPARAM
    });
    
    stage.xProperty().addListener(_ -> {
      pricecompareGuiUpdateActor ! GuiUpdateActor.SAVEGUIPARAM
    });

    stage.yProperty().addListener(_ -> {
      pricecompareGuiUpdateActor ! GuiUpdateActor.SAVEGUIPARAM
    });
  
    stage.delegate.getIcons().add(new Image("pricecompare.png"))
    
    stage.delegate.setOnCloseRequest(new javafx.event.EventHandler[javafx.stage.WindowEvent]() {
        def handle(e: javafx.stage.WindowEvent): Unit = {
          stopAll()
        }
    })
    
    
    //------------------------------- prepare files -------------------------------
    ("notfound.html".toFile).overwrite("<html><body>")(charset = "UTF-8")
    
    ("pricechanged.html".toFile).overwrite("<html><body>")(charset = "UTF-8")
    
    loadGuiConfig(system, pricecompareGuiUpdateActor, guiconfigFile)
    
    checkfile(Urlfile.theUrlFile())
    checkfile(extractorFileName)
    checkfile(alertSoundFile)
    checkfile(guiconfigFile)
    
    Thread.sleep(500) // ?
    pricecompareGuiUpdateActor ! GuiUpdateActor.TA_SCROLLTOTOP
    
    if (Guidata.autostart){
      compare(Urlfile.theUrlFile())
    }
  }
  //--------------------------------- start end ---------------------------------
  

  
  //--------------------------------- checkfile ---------------------------------
  def checkfile(filename: String) = {
    if (!((filename.toFile).exists)) pricecompareGuiUpdateActor ! GuiUpdateActor.Ta_add("Error, missing file: " + filename + "\n\n\n")
  }

  //------------------------------- loadGuiConfig -------------------------------
  def loadGuiConfig(system: ActorSystem, pricecompareGuiUpdateActor: akka.actor.ActorRef, guiconfigFile: String) = {
    var guiConfigXML = new scala.xml.Elem(null, "root", scala.xml.Null , scala.xml.TopScope, false)

    val guiConfigFileLocal = guiconfigFile
    if (new JFile(guiConfigFileLocal).exists) {
      guiConfigXML = Try(XML.loadFile(guiConfigFileLocal)) getOrElse new scala.xml.Elem(null, "root", scala.xml.Null , scala.xml.TopScope, false)
      Guidata.frameWidth = (Try((guiConfigXML \\ "mainframe_width").text) getOrElse cnf.widthDefault.toString).toInt
      Guidata.frameHeight = (Try((guiConfigXML \\ "mainframe_height").text) getOrElse cnf.heightDefault.toString).toInt
      Guidata.framePosX = (Try((guiConfigXML \\ "mainframe_position_x").text) getOrElse cnf.topDefault.toString).toInt
      Guidata.framePosY = (Try((guiConfigXML \\ "mainframe_position_y").text) getOrElse cnf.leftDefault.toString).toInt
      
      system.scheduler.scheduleOnce(1 seconds, pricecompareGuiUpdateActor, SETWIDTH(Guidata.frameWidth))
      system.scheduler.scheduleOnce(1 seconds, pricecompareGuiUpdateActor, SETHEIGHT(Guidata.frameHeight))
      system.scheduler.scheduleOnce(1 seconds, pricecompareGuiUpdateActor, SETX(Guidata.framePosX))
      system.scheduler.scheduleOnce(1 seconds, pricecompareGuiUpdateActor, SETY(Guidata.framePosY))
    }
  }
  
  //------------------------------- saveGuiConfig -------------------------------
  def saveGuiConfig(stage: Stage) = {
    var guiConfigXML = new scala.xml.Elem(null, "root", scala.xml.Null , scala.xml.TopScope, false)
    
    val guiConfigFile = "guiconfig.xml"

    if (new JFile(guiConfigFile).exists) {
      guiConfigXML = Try(XML.loadFile(guiConfigFile)) getOrElse new scala.xml.Elem(null, "root", scala.xml.Null , scala.xml.TopScope, false)
    }

    val nb = new scala.xml.NodeBuffer
    val oldchilds = guiConfigXML \\ "root" \ "_"

    oldchilds foreach (n => nb += n)

    var rm = oldchilds \\ "mainframe_position_x"
    if (rm.length > 0) rm foreach (n => nb -= n)

    rm = oldchilds \\ "mainframe_position_y"
    if (rm.length > 0) rm foreach (n => nb -= n)

    rm = oldchilds \\ "mainframe_width"
    if (rm.length > 0) rm foreach (n => nb -= n)

    rm = oldchilds \\ "mainframe_height"
    if (rm.length > 0) rm foreach (n => nb -= n)

    nb += <mainframe_position_x>{stage.getX().toInt}</mainframe_position_x>
    nb += <mainframe_position_y>{stage.getY().toInt}</mainframe_position_y>
    nb += <mainframe_width>{stage.getWidth().toInt}</mainframe_width>
    nb += <mainframe_height>{stage.getHeight().toInt}</mainframe_height>

    val new_guiConfigXML = <root>{nb}</root>

    val prettyPrinter = new scala.xml.PrettyPrinter(120, 2)
    val prettyXml = prettyPrinter.format(new_guiConfigXML)
    
    val file: File = guiConfigFile.toFile
    file.overwrite(prettyXml)(charset = "UTF-8")
  }
  
  //--------------------------- closeAndAddSuppressed ---------------------------
  def closeAndAddSuppressed(e: Throwable, resource: AutoCloseable): Unit = {
    if (e != null) {
      try {
        resource.close()
      } catch {
        case NonFatal(suppressed) => e.addSuppressed(suppressed)
      }
    } else {
      resource.close()
    }
  }
  
  //------------------------------ showAlertError ------------------------------
  def showAlertError(m: String) = {
    runLater( new Runnable() {
      override def run() = {
        new Alert(AlertType.Error, m){}.showAndWait()
      }
    })
  }
  
  //------------------------------- showAlertInfo -------------------------------
  def showAlertInfo(m: String) = {
    runLater( new Runnable() {
      override def run() = {
        new Alert(AlertType.Information, m){}.showAndWait()
      }
    })
  }
  //-------------------------------- readUrlFile --------------------------------
  def readUrlFile(urlString: String) = {
    var linesUrl: Either[String, List[String]] = Left("")
    var ll = 0
    try {
      val lines = urlString.toFile.lineIterator.toList.map(_.strip).filter(!_.isEmpty())
      ll = lines.length
      linesUrl = Right(lines)
    } catch {
      case NonFatal(e@_) =>
        ll = 0
        linesUrl= Left(s"Problem with URL-file $urlString ($e)!")
    }
    (linesUrl, ll)
  }
  
  //--------------------------------- compare() ---------------------------------
  def compare(urlFileName: String) = {
    rowsBuffer.clear()
    
    val fileName = "notfound.html" //clear
    val file: File = fileName.toFile
    file.overwrite("<html><body>")(charset = "UTF-8")
  
    val t = readUrlFile(urlFileName)
    Guidata.rowsMAX = t._2
      
    t._1 match {
      case Right(x) => {
        for (i <- 0 until Guidata.rowsMAX) rowsBuffer += new PricecompareResultRow((i + 1).toString, "", "", "", Color.White)
        readExtractorFile(extractorFileName) match {
          case Right(y) => pricecompareWorkerActor ! WorkerActor.Compare(x, y)
          case Left(y) => {
            pricecompareGuiUpdateActor ! GuiUpdateActor.Ta_add(s"\n$y \n")
            // PricecompareLogger.log_error(s"\n$y \n")
            showAlertInfo(s"\n$y \n")
          }
        }
      }
      case Left(x) => {
        pricecompareGuiUpdateActor ! GuiUpdateActor.Ta_add("\n" + x + "\n")
        // PricecompareLogger.log_error("\n" + x + "\n")
        showAlertInfo(x)
      }
    }
  }
//----------------------------- readExtractorFile -----------------------------
  def readExtractorFile(extractorFileName: String) = {
    var mapExtractor: Either[String, Map[String, String]] = Left("Error in readExtractorFile() subroutine!")
    
    try {
      val m = extractorFileName.toFile.lineIterator.filter(_ != "").filter(_.contains("~")).toList.map(x => x.split("~")(0) -> x.split("~")(1)).toMap
      mapExtractor = Right(m)
    } catch {
      case NonFatal(e@_) =>
        val msg = s"Problem with Extractor-file $extractorFileName: " + e
        mapExtractor = Left(msg)
    }
    mapExtractor
  }
  //---------------------------------- safely ----------------------------------
  def safely[T](handler: PartialFunction[Throwable, T]): PartialFunction[Throwable, T] = {
    case ex: scala.util.control.ControlThrowable => throw ex
    // case ex: OutOfMemoryError (Assorted other nasty exceptions you don't want to catch)
    
    //If it's an exception they handle, pass it on
    case ex: Throwable if handler.isDefinedAt(ex) => handler(ex)
    
    // If they didn't handle it, rethrow. This line isn't necessary, just for clarity
    case ex: Throwable => throw ex
  }

}


//#END Pricecompare #############################################################


