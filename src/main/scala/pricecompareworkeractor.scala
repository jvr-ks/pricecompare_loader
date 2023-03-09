/************************************************
* pricecompareworkeractor.scala
************************************************/
//ü

package de.jvr.pricecompare


//import scala.concurrent._

import akka.actor.Actor

import better.files._
import scala.util.{Try, Success, Failure}

import de.jvr.pricecompare.Pricecompare._
import de.jvr.pricecompare.GuiUpdateActor._

import scalafx.scene.paint.Color


//#object WorkerActor ####################################################
object WorkerActor {
  
  final case class Compare(x:List[String], y: Map[String, String])
  final case class ReadUrlResult(i: Int, theUrl: String, price: String, remark: String, mapExtractor: Map[String, String], result: String)
}

//#class WorkerActor(magicNumber: Int) ###################################
class WorkerActor extends Actor {
  import WorkerActor._
  
  var comparePosition = 0
  var linecounter = 0
  
  def receive = {
    
    case Compare(linesUrl: List[String], mapExtractor: Map[String, String]) => {
      pricecompareGuiUpdateActor ! GuiUpdateActor.Ta_addCLEAR("Comparing, takes a while, please be patient... \n")
      if(Urlfile.fastmode) pricecompareGuiUpdateActor ! GuiUpdateActor.Ta_add("Fastmode!\n")
      
      if (Urlfile.theUrlFile() != "pricecompare_urls.txt")
        pricecompareGuiUpdateActor ! GuiUpdateActor.Ta_add(s"Using UrlFile: ${Urlfile.theUrlFile()}\n")
      
      for (i <- comparePosition until linesUrl.length){
        val line = linesUrl(i)
        if (line.toLowerCase().contains("http")){
          val lineRex = """(http.*?[-a-zA-Z0-9@:%._\+~#=]{2,256}\.[a-z]{2,4}\b[-a-zA-Z0-9@:%_\+.~#?&//=()]*) +((?:\d+[.,]\d{1,2})|(?:\d+ ))(.*)""".r
          
          val theUrlTry = {Try{lineRex.findAllIn(line).matchData.toList(0).group(1)}}
          val price = {Try{lineRex.findAllIn(line).matchData.toList(0).group(2)} getOrElse "0,00"}
          val remark = {Try{lineRex.findAllIn(line).matchData.toList(0).group(3)} getOrElse ""}
          
          val codec = if (remark.toLowerCase.contains("cp1252")) "ISO-8859-1" else "UTF-8"
          
          theUrlTry match {
            case Failure(_) => pricecompareGuiUpdateActor ! GuiUpdateActor.Ta_add(s"No. ${i + 1}: Error in definition of the line: $line\n")
            case Success(theUrl) => {
              if (Urlfile.fastmode){
                pricecompareReadUrlActor ! ReadUrlActor.ReadUrlFast(i, theUrl, price, remark, mapExtractor, codec)
              } else {
                pricecompareReadUrlActor ! ReadUrlActor.ReadUrl(i, theUrl, price, remark, mapExtractor, codec)
              }
            }
          }
        }
      }
    }
    
    case ReadUrlResult(i, theUrl, price, remark, mapExtractor, result) => {
      val html = result
      //log_debug(theUrl + ":\n")
      //log_debug("First 100 characters of contents: \n" + html.substring(0, math.min(100, html.length)))
      //log_debug("Complete contents: \n" + html)
      var toSearch = ""
      val domainRex = """https:\/\/([\w.\?0-9]+)""" //"

      val urlHasDomain = (domainRex.r("domainName").unanchored).findFirstMatchIn(theUrl)
      if (urlHasDomain.isDefined) {
        val domain = urlHasDomain.get.group("domainName")
        //log_debug("domain: " + domain + " \n")
        if (theUrl.contains(domain)) toSearch = mapExtractor(domain)
        
        //log_debug("toSearch: " + toSearch + " \n")
        val pattern = toSearch.r("webprice").unanchored
        val result = pattern.findFirstMatchIn(html)
        val urlfield = if (remark == "") theUrl else theUrl + " (" + remark + ")"
        
        if (result.isDefined) {
          val r = result.get.group("webprice")
          if (r.replace(",",".").toDouble == price.replace(",",".").toDouble){
            pricecompareGuiUpdateActor ! Addrow(i, new PricecompareResultRow((i + 1).toString, urlfield, price, r, Color.LightGreen))
          } else {
            pricecompareGuiUpdateActor ! Addrow(i, new PricecompareResultRow((i + 1).toString, urlfield, price, r, Color.Red))
            pricecompareGuiUpdateActor ! ALERTSOUND
            ("pricechanged.html".toFile).appendLine().append((i + 1).toString + " <a href=" + urlfield + ">" + urlfield + "</a> Price changed, was: " + price + " is now: " + r + "<br />\n")
          }
          pricecompareGuiUpdateActor ! Scrolldown(i, pricecompareResultView, Guidata.viewlines)
        } else {
          pricecompareGuiUpdateActor ! Addrow(i, new PricecompareResultRow((i + 1).toString, urlfield, price, " not found!", Color.Red))
          
          ("notfound.html".toFile).appendLine().append((i + 1).toString + " <a href=" + urlfield + ">" + urlfield + "</a> webpage not found!<br />\n")
          pricecompareGuiUpdateActor ! Scrolldown(i, pricecompareResultView, Guidata.viewlines)
          if (Guidata.sound) pricecompareGuiUpdateActor ! ALERTSOUND
        }
      }
      if (i >= Guidata.rowsMAX - 1) {
        if (!Urlfile.fastmode) {
          pricecompareGuiUpdateActor ! GuiUpdateActor.Ta_add(s"All requests (${i + 1}) sent!\n")
          Urlfile.running = false
        }
      }
    }

    case e: akka.actor.Status.Failure => pricecompareGuiUpdateActor ! GuiUpdateActor.Ta_add("Error: "+ e.toString)
    
    case _ => //logger.error("WorkerActor received unknown command!")

  }
  
}

//#END WorkerActor #######################################################
//
