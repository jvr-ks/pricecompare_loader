/************************************************
* pricecomparelog.scala
*
* License GNU GENERAL PUBLIC LICENSE see License.txt
************************************************/
// ü

package de.jvr.pricecompare

import better.files._
import java.io.{File => JFile}

import com.typesafe.scalalogging._


object PricecompareLogger {

	private val log = Logger(Pricecompare.getClass)
	
	val wrkDir = System.getProperty("user.dir") + JFile.separator
	
	def log_trace(msg: String)(implicit line: sourcecode.Line, file: sourcecode.File) = {
		val p = file.value.toFile.path.toString.replace(wrkDir,"[wrkDir]")
		log.trace(s"\n$msg line: ${line.value} in: ${p}\n")
	}
	
	def log_debug(msg: String)(implicit line: sourcecode.Line, file: sourcecode.File) = {
		val p = file.value.toFile.path.toString.replace(wrkDir,"[wrkDir]")
		log.debug(s"\n$msg line: ${line.value} in: ${p}\n")
	}

	def log_info(msg: String)(implicit line: sourcecode.Line, file: sourcecode.File) = {
		val p = file.value.toFile.path.toString.replace(wrkDir,"[wrkDir]")
		log.info(s"\n$msg line: ${line.value} in: ${p}\n")
	}
	
	def log_warn(msg: String)(implicit line: sourcecode.Line, file: sourcecode.File) = {
		val p = file.value.toFile.path.toString.replace(wrkDir,"[wrkDir]")
		log.warn(s"\n$msg line: ${line.value} in: ${p}\n")
	}
	
	def log_error(msg: String)(implicit line: sourcecode.Line, file: sourcecode.File) = {
		val p = file.value.toFile.path.toString.replace(wrkDir,"[wrkDir]")
		log.error(s"\n\n$msg line: ${line.value} in: ${p}\n\n")
	}
	
}
