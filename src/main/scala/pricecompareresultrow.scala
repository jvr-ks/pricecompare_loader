/************************************************
* pricecompareresultrow.scala
************************************************/
//ü

package de.jvr.pricecompare

import scalafx.beans.property.StringProperty
import scalafx.beans.property.ObjectProperty
import scalafx.scene.paint.Color

class PricecompareResultRow(index_ : String, url_ : String, priceList_ : String, priceWeb_ : String, okColor_ : Color) {
  val index = new StringProperty(this, "Index", index_)
  val url = new StringProperty(this, "Url", url_)
  val priceList = new StringProperty(this, "List", priceList_)
  val priceWeb = new StringProperty(this, "Web", priceWeb_)
  val okColor = new ObjectProperty(this, "okColor", okColor_)
}