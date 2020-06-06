package com.work.space.crawler

import scala.io.Source

object TestHelper {

  def file2String(filePath: String): String = {
    val file = Source.fromFile(filePath)
    val resp = file.mkString
    file.close()
    resp
  }


}
