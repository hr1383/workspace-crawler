package com.vicaya.app.response

object ConnectorEnum extends Enumeration {
  type Connector = Value
  val GDRIVE, CONFLUENCE, DROPBOX, JIRA = Value
}

case class Document(source: String,
                    filename: String = "",
                    url: String = "",
                    author: String = "",
                    id: String = "",
                    title: String = "",
                    description: String = "")

