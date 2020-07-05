package com.vicaya.connectors

import com.google.api.services.drive.Drive
import com.vicaya.app.response.{ConnectorEnum, Document}


object GDriveConnect {


//  def main(args: Array[String]): Unit = {
//    val driveConnect = new GDriveConnect()
//    driveConnect.searchContent("curl")
//  }
}

class GDriveConnect(service: Drive) extends SearchConnect {

  def searchContent(text: String, pageSize: Int = 10): Seq[Document] = {

    // Print the names and IDs for up to 10 files.
    val result = service.files.list
      .setQ(s"fullText contains \'$text\'")
      .setPageSize(pageSize)
      // .setCorpora("drive") //this parameter is to define the scope for sarch
      .setFields("nextPageToken, files(id, name)")
      .execute
    val files = result.getFiles
    if (files == null || files.isEmpty) {
      System.out.println("No files found.")
      Seq.empty
    }
    else {
      import com.google.api.services.drive.model.File
      import scala.collection.JavaConverters
      val sequence: Seq[File] = JavaConverters.asScalaIteratorConverter(files.iterator()).asScala.toSeq
      sequence.map(f => Document(source = ConnectorEnum.GDRIVE.toString,
        filename = f.getName,
        id = f.getId))
    }
  }


}
