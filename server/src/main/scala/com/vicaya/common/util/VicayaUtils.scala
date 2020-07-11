package com.vicaya.common.util

import scala.io.Source
import scala.util.{Failure, Success, Try}

object VicayaUtils {

  def readFileContent(fileName: String): String = {
    Try(Source.fromFile(fileName).mkString) match {
      case Success(content) =>
        content
      case Failure(ex) =>
        println(s"Failed to read account sql file", ex)
        ""
    }
  }
}
