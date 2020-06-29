package com.vicaya.connectors

import com.vicaya.app.response.Document

trait SearchConnect {

  def searchContent(text: String, pageSize: Int = 10): Seq[Document]
}
