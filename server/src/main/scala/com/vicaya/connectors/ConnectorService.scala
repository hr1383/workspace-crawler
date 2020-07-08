package com.vicaya.connectors

import com.vicaya.app.response.Document


object ConnectorService {

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

  def search(text: String): Seq[Document] = {
    System.out.println("searching for " + text)
    val gDocument = gDriveConnect.searchContent(text)
    //add more connectors
    val confluenceDocument = confluenceConnect.searchContent(text)
    gDocument ++ confluenceDocument ++ jiraConnect.searchContent(text) ++ boxConnect.searchContent(text)
  }

}
