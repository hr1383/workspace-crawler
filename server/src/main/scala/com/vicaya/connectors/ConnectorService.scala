package com.vicaya.connectors

import com.vicaya.app.response.Document


object ConnectorService {

  def apply(gDriveConnect: GDriveConnect,
            confluenceConnect: ConfluenceConnect,
            jiraConnect: JiraConnect): ConnectorService = new ConnectorService(gDriveConnect, confluenceConnect, jiraConnect)
}

class ConnectorService(gDriveConnect: GDriveConnect,
                       confluenceConnect: ConfluenceConnect,
                       jiraConnect: JiraConnect) {

  def search(text: String): Seq[Document] = {
    System.out.println("searching for " + text)
    val gDocument = gDriveConnect.searchContent(text)
    //add more connectors
    val confluenceDocument = confluenceConnect.searchContent(text)
    System.out.println("size " + gDocument.size + "  " + confluenceDocument.size)
    gDocument ++ confluenceDocument ++ jiraConnect.searchContent(text)
  }

}
