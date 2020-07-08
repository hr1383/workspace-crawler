package com.vicaya.connectors

import com.vicaya.app.response.Document
import org.slf4j.{Logger, LoggerFactory}


object ConnectorService {
  val logger: Logger = LoggerFactory.getLogger("com.vicaya.connectors.ConnectorService")
  def apply(gDriveConnect: GDriveConnect,
            confluenceConnect: ConfluenceConnect,
            jiraConnect: JiraConnect,
            boxConnect: BoxConnect): ConnectorService = new ConnectorService(gDriveConnect,
    confluenceConnect, jiraConnect, boxConnect)
}

class ConnectorService(gDriveConnect: GDriveConnect,
                       confluenceConnect: ConfluenceConnect,
                       jiraConnect: JiraConnect,
                       boxConnect: BoxConnect) {
  import ConnectorService._

  def search(text: String): Seq[Document] = {
    logger.info("searching for " + text)
    val gDocument = gDriveConnect.searchContent(text)
    //add more connectors
    val confluenceDocument = confluenceConnect.searchContent(text)
    gDocument ++ confluenceDocument ++ jiraConnect.searchContent(text) ++ boxConnect.searchContent(text)
  }

}
