package com.work.space.crawler.util

case object WorkSpaceCrawlerUtil {
  final val defaultEmptySeq = Seq.empty[String]
  final val defaultEmptyConf = Map[String, String]()

  def buildResourceLookupUrl(host: String): String = {
    s"http://$host:8080/json/"
  }

  def convertToMB(memory: String): Double = {
    val unit = memory.takeRight(1)
    val number = memory.dropRight(1).toInt

    unit match {
      case "k" => number/1024.0
      case "m" => number
      case "g" => number*1024
      case "t" => number*1024*1024
      case _ => throw new IllegalArgumentException("Invalid format for memort. Only 'k' for KB, 'm' for MB, 'g' for GB or 't' for TB must be used. e.g 512m")
    }
  }
}
