/************************************************
* pricecomparereadurlactor.scala
************************************************/
//ü

package de.jvr.pricecompare

//import scala.concurrent._

import akka.actor.Actor
import scala.concurrent.Future

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements

import scala.jdk.CollectionConverters._

import java.io.IOException


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
      
      if (theUrl.length() > 5){
        try {
          val response: scalaj.http.HttpResponse[Unit] = Http(theUrl).option(HttpOptions.followRedirects(true)).charset(codec).option(HttpOptions.readTimeout(Urlfile.timeout)).execute(is => {
            result = scala.io.Source.fromInputStream(is).getLines().mkString("\n")
          })
          
          val rc = response.code
          rc match {
            case 200 => pricecompareWorkerActor ! WorkerActor.ReadUrlResult(i, theUrl, price, remark, mapExtractor, result)
            case 404 => pricecompareGuiUpdateActor ! GuiUpdateActor.Ta_add(s"No. ${i + 1}: $theUrl errorcode is: 404 (file not found!)\n")
            case c: Int => pricecompareGuiUpdateActor ! GuiUpdateActor.Ta_add(s"No. ${i + 1}: $theUrl errorcode is: $c\n")
          }
        } catch {
          case _: Throwable => {
            try {
              val uAgent = """Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:103.0) Gecko/20100101 Firefox/103.0"""
              val cookies = Map("_cookie_consent" -> "eyJGVU5DVElPTkFMIjp7ImFsbG93ZWQiOnRydWV9LCJQUkVGRVJFTkNFUyI6eyJhbGxvd2VkIjp0cnVlfSwiQU5BTFlUSUNTIjp7ImFsbG93ZWQiOnRydWV9LCJBRFZFUlRJU0lORyI6eyJhbGxvd2VkIjp0cnVlfSwiVFJBQ0tJTkciOnsiYWxsb3dlZCI6dHJ1ZX0sIk9USEVSIjp7ImFsbG93ZWQiOnRydWV9fQ%3D%3D", "x227d2" -> "5ad2139324c7eabd0750ec6539f86130").asJava
              val result = Jsoup.connect(theUrl)
                          .userAgent(uAgent)
                          .ignoreHttpErrors(true)
                          .cookies(cookies)
                          .get()
              
              pricecompareWorkerActor ! WorkerActor.ReadUrlResult(i, theUrl, price, remark, mapExtractor, result.toString())
              pricecompareGuiUpdateActor ! GuiUpdateActor.Ta_add(s"Using Jsoup to fetch page: $theUrl\n")
            } catch {
              case e: Throwable => { // org.jsoup.HttpStatusException
                pricecompareWorkerActor ! WorkerActor.ReadUrlResult(i, theUrl, "-", "-", mapExtractor, "-")
                pricecompareGuiUpdateActor ! GuiUpdateActor.Ta_add(s"$i: Error $e opening page: $theUrl\n")
              }
            }
          }
        }
      }
    }
    
    case ReadUrlFast(i, theUrl, price, remark, mapExtractor, codec) => {
      if (theUrl.length() > 5){
        val responseF = Future {
          var result = ""
        
          val response: scalaj.http.HttpResponse[Unit] = Http(theUrl).option(HttpOptions.followRedirects(true)).charset(codec).option(HttpOptions.readTimeout(Urlfile.timeout)).execute(is => {
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
          case Failure(e) => {
            try {
              val uAgent = """Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:103.0) Gecko/20100101 Firefox/103.0"""
              val cookies = Map("_cookie_consent" -> "eyJGVU5DVElPTkFMIjp7ImFsbG93ZWQiOnRydWV9LCJQUkVGRVJFTkNFUyI6eyJhbGxvd2VkIjp0cnVlfSwiQU5BTFlUSUNTIjp7ImFsbG93ZWQiOnRydWV9LCJBRFZFUlRJU0lORyI6eyJhbGxvd2VkIjp0cnVlfSwiVFJBQ0tJTkciOnsiYWxsb3dlZCI6dHJ1ZX0sIk9USEVSIjp7ImFsbG93ZWQiOnRydWV9fQ%3D%3D", "x227d2" -> "5ad2139324c7eabd0750ec6539f86130").asJava
              val result = Jsoup.connect(theUrl)
                          .userAgent(uAgent)
                          .ignoreHttpErrors(true)
                          .cookies(cookies)
                          .get()
              
              pricecompareWorkerActor ! WorkerActor.ReadUrlResult(i, theUrl, price, remark, mapExtractor, result.toString())
              pricecompareGuiUpdateActor ! GuiUpdateActor.Ta_add(s"Using Jsoup to fetch page: $theUrl\n")
            } catch {
              case e: Throwable => { // org.jsoup.HttpStatusException
                pricecompareWorkerActor ! WorkerActor.ReadUrlResult(i, theUrl, "-", "-", mapExtractor, "-")
                pricecompareGuiUpdateActor ! GuiUpdateActor.Ta_add(s"$i: Error $e opening page: $theUrl\n")
              }
            }
          }
        }
      }
    }
    
    case m => pricecompareGuiUpdateActor ! GuiUpdateActor.Ta_add("ReadUrlActor received unknown command: " + m)
  }
}

// TODO dispatch.Http.default.shutdown()