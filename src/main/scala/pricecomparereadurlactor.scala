/************************************************
* pricecomparereadurlactor.scala
************************************************/
//ü

package de.jvr.pricecompare

//import scala.concurrent._

import akka.actor.Actor
import scala.concurrent.Future

import de.jvr.pricecompare.Pricecompare._
//import de.jvr.pricecompare.WorkerActor._

import scalaj.http._
import scala.util.{Success,Failure}

//------------------------------- ReadUrlActor -------------------------------
object ReadUrlActor {
	//def props = Props[ReadUrlActor]
	final case class Finished()
	final case class ReadUrl(i: Int, url: String, price: String, remark: String, mapExtractor: Map[String, String], codec: String)
	final case class ReadUrlFast(i: Int, url: String, price: String, remark: String, mapExtractor: Map[String, String], codec: String)
}
//------------------------------- ReadUrlActor -------------------------------
class ReadUrlActor extends Actor{
	import ReadUrlActor._
	
	// First set the default cookie manager.
	java.net.CookieHandler.setDefault(new java.net.CookieManager(null, java.net.CookiePolicy.ACCEPT_ALL))
	
	def receive = {
		
		case ReadUrl(i, theUrl, price, remark, mapExtractor, codec) => {
			
			var result = ""
			val timeout: Int = 60000
			
			val response: scalaj.http.HttpResponse[Unit] = Http(theUrl).option(HttpOptions.followRedirects(true)).charset(codec).option(HttpOptions.readTimeout(timeout)).execute(is => {
				result = scala.io.Source.fromInputStream(is).getLines().mkString("\n")
			})
			
			val rc = response.code
			rc match {
				case 200 => pricecompareWorkerActor ! WorkerActor.ReadUrlResult(i, theUrl, price, remark, mapExtractor, result)
				case 404 => pricecompareGuiUpdateActor ! GuiUpdateActor.Ta_add(s"No. ${i + 1}: $theUrl errorcode is: 404 (file not found!)\n")
				case c: Int => pricecompareGuiUpdateActor ! GuiUpdateActor.Ta_add(s"No. ${i + 1}: $theUrl errorcode is: $c\n")
			}
		}
		
		case ReadUrlFast(i, theUrl, price, remark, mapExtractor, codec) => {
			val responseF = Future{
				var result = ""
				val timeout: Int = 10000
			
				val response: scalaj.http.HttpResponse[Unit] = Http(theUrl).option(HttpOptions.followRedirects(true)).charset(codec).option(HttpOptions.readTimeout(timeout)).execute(is => {
					result = scala.io.Source.fromInputStream(is).getLines().mkString("\n")
				})
				(response.code,result)
			}
			
			responseF.onComplete {
				case Success(x) => {
					x match {
						case (200,s) => pricecompareWorkerActor ! WorkerActor.ReadUrlResult(i, theUrl, price, remark, mapExtractor, s)
						case (404,_) => pricecompareGuiUpdateActor ! GuiUpdateActor.Ta_add(s"No. ${i + 1}: $theUrl errorcode is: 404 (file not found!)\n")
						case (c: Int,_) => pricecompareGuiUpdateActor ! GuiUpdateActor.Ta_add(s"No. ${i + 1}: $theUrl errorcode is: $c\n")
					}
				}
				case Failure(e) => pricecompareWorkerActor ! WorkerActor.ReadUrlResult(i, theUrl, price, s"Got error: ${e.toString}", mapExtractor, "")
			}
		}
		
		case m => pricecompareGuiUpdateActor ! GuiUpdateActor.Ta_add("ReadUrlActor received unknown command: " + m)
	}
}

// TODO dispatch.Http.default.shutdown()