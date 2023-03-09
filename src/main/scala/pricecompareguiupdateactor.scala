/************************************************
* pricecompareguiupdateactor.scala
************************************************/
//ü

package de.jvr.pricecompare

import scala.io.Source

import akka.actor.Actor
 
import scalafx.scene.control._
import scalafx.scene.control.Alert.AlertType

import java.io.{File => JFile}

import javafx.application.Platform._
import javafx.scene.media.Media
import javafx.scene.media.MediaPlayer

//import org.log4s._

import de.jvr.pricecompare.Pricecompare._


//#object GuiUpdateActor ####################################################
object GuiUpdateActor {
  //def props = Props[GuiUpdateActor]
  
  final case class Ta_replace(s: String)
  final case class Ta_add(s: String)
  final case class Ta_addCLEAR(s: String)
  final case object TA_SCROLLTOTOP
  final case object TA_SCROLLTOBOTTOM
  
  final case class SETWIDTH(n: Double)
  final case class SETHEIGHT(n: Double)
  final case class SETX(n: Double)
  final case class SETY(n: Double)
  
  final case class Addrow(i: Int, row: PricecompareResultRow)
  final case class Scrolldown(i: Int, tv: TableView[PricecompareResultRow], lines: Int)

  final case object CHECKVERSION
  final case object SHUTDOWN
  final case object ALERTSOUND

  final case object SAVEGUIPARAM
}


//#class GuiUpdateActor ###################################
class GuiUpdateActor extends Actor {
  import GuiUpdateActor._
  
  def onFX(body : =>Unit) = {
    runLater( new Runnable() {
      override def run() = body
    })
  }
  
  var justSaved = List[String]("")

  def receive = {
    case Ta_replace( t: String ) => {
      onFX({
        ta.text = t
      })
    }
    
    case Ta_add( t: String ) => {
      onFX({
        ta.text = ta.text.get() + t
        ta.delegate.positionCaret( ta.text.get().length )
        
      })
    }
    
    case Ta_addCLEAR( t: String ) => {
      onFX({
        ta.text = ""
        ta.text = ta.text.get() + t
        ta.delegate.positionCaret( ta.text.get().length )
      })
    }
    
    case TA_SCROLLTOTOP => {
      onFX({
        ta.positionCaret(0)
      })
    }
    
    case TA_SCROLLTOBOTTOM => {
      onFX({
        ta.positionCaret(ta.getText().length())
      })
    }
    
    case CHECKVERSION => {
      onFX({
        try {
          val html = Source.fromURL( versionsurl ).mkString
          
          val toSearch = """<li>""" + appnameUpper + """ (\d*.\d*)</li>"""
          
          val pattern = toSearch.r("onlineVersion").unanchored
          
          val result = pattern.findFirstMatchIn(html)
          
          if (result.isDefined) {
            val r = result.get.group("onlineVersion")

            if (r.toDouble > version.toDouble){
              val alert = new Alert(AlertType.Confirmation) {
                initOwner(stage)
                title = "Confirmation Dialog"
                headerText = "Newer version available, visit downloadpage?"
                contentText = ""
              }
              
              val result = alert.showAndWait()
              
              result match {
                case Some(ButtonType.OK) => openURL(updateUrl)
                case _                   => 
              }
            } else {
              showAlertInfo("No new version available!")
            }
          } else {
            pricecompareGuiUpdateActor ! GuiUpdateActor.Ta_replace("Problem reading online version-info!")
          }
        } catch {
          safely {
            ex: Throwable =>  new Alert(AlertType.Error) {
              initOwner(stage)
              title = "Error occured"
              headerText = "Get version info is not possible!"
              contentText = ex.toString
            }.showAndWait()
          }
        }
      })
    }

    case SAVEGUIPARAM => {
      saveGuiConfig(stage)
    }


    case SETWIDTH(n: Double) => {
      onFX({
        stage.setWidth(n)
      })
    }

    case SETHEIGHT(n: Double) => {
      onFX({
        stage.setHeight(n)
      })
    }

    case SETX(n: Double) => {
      onFX({
        stage.setX(n)
      })
    }

    case SETY(n: Double) => {
      onFX({
        stage.setY(n)
      })
    }

    case SHUTDOWN => {
      stopAll()
    }

    case ALERTSOUND => {
      if(Guidata.sound) {
        try {
          var mediaPlayer = new MediaPlayer(new Media(new JFile("alertsound.mp3").toURI().toString()))
          mediaPlayer.play()
          Thread.sleep(5000)
          mediaPlayer.stop()
          mediaPlayer = null
        } catch {
          safely {
            ex: Throwable => java.awt.Toolkit.getDefaultToolkit().beep()
          }
        }
      }
    }
    
    case Addrow(i: Int, row: PricecompareResultRow) => {
      onFX({
        rowsBuffer.remove(i, 1)
        rowsBuffer.insert(i, row)
      })
    }
    
    case Scrolldown(i: Int, tv: TableView[PricecompareResultRow], lines: Int) => {
      onFX({
        if (i > lines + 1){
          tv.scrollTo(i - lines)
        }
      })
    }
    
    case _ => ;

  }
}




