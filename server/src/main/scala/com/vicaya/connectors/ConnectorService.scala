package com.vicaya.connectors

import com.vicaya.app.response.Document
import org.slf4j.{Logger, LoggerFactory}


object ConnectorService {
  val logger: Logger = LoggerFactory.getLogger("com.vicaya.connectors.ConnectorService")
  def apply(gDriveConnect: GDriveConnect,
            confluenceConnect: ConfluenceConnect,
            jiraConnect: JiraConnect): ConnectorService = new ConnectorService(gDriveConnect, confluenceConnect, jiraConnect)
}

class ConnectorService(gDriveConnect: GDriveConnect,
                       confluenceConnect: ConfluenceConnect,
                       jiraConnect: JiraConnect) {
  import ConnectorService._
  def search(text: String): Seq[Document] = {
    logger.info("searching for " + text)
    val gDocument = gDriveConnect.searchContent(text)
    //add more connectors
    val confluenceDocument = confluenceConnect.searchContent(text)
    logger.info("size " + gDocument.size + "  " + confluenceDocument.size)
    gDocument ++ confluenceDocument ++ jiraConnect.searchContent(text)
  }

}
