package com.vicaya.connectors

import com.vicaya.app.response.Document


object ConnectorService {

  def apply(gDriveConnect: GDriveConnect): ConnectorService = new ConnectorService(gDriveConnect)
}

class ConnectorService(gDriveConnect: GDriveConnect) {

  def search(text: String): Seq[Document] = {
    val gDocument = gDriveConnect.searchContent(text)
    //add more connectors
    gDocument
  }

}
