package com.vicaya.connectors

import java.io.{File, FileNotFoundException, IOException, InputStreamReader}
import java.util.Collections

import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver
import com.google.api.client.googleapis.auth.oauth2.{GoogleAuthorizationCodeFlow, GoogleClientSecrets}
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.util.store.FileDataStoreFactory
import com.google.api.services.drive.{Drive, DriveScopes}
import com.vicaya.app.response.Document


object GDriveConnect {
  private val APPLICATION_NAME = "Google Drive API Search Content"
  private val JSON_FACTORY = JacksonFactory.getDefaultInstance
  private val TOKENS_DIRECTORY_PATH = "tokens"
  /**
   * Global instance of the scopes required by this quickstart.
   * If modifying these scopes, delete your previously saved tokens/ folder.
   */
  private val SCOPES = Collections.singletonList(DriveScopes.DRIVE_METADATA_READONLY)
  private val CREDENTIALS_FILE_PATH = "/credentials.json"
  private val HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport
  var service: Drive = _

  @throws[IOException]
  private def getCredentials(HTTP_TRANSPORT: NetHttpTransport) = { // Load client secrets.
    val in = classOf[Nothing].getResourceAsStream(CREDENTIALS_FILE_PATH)
    if (in == null) throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH)
    val clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in))
    // Build flow and trigger user authorization request.
    val flow = new GoogleAuthorizationCodeFlow.Builder(HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
      .setDataStoreFactory(new FileDataStoreFactory(new File(TOKENS_DIRECTORY_PATH))).setAccessType("offline").build
    val receiver = new LocalServerReceiver.Builder().setPort(8888).build
    new AuthorizationCodeInstalledApp(flow, receiver).authorize("user")
  }

  def getService(): Drive = {
    if (service == null) {
      synchronized {
        if (service == null)
          service = new Drive
          .Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
            .setApplicationName(APPLICATION_NAME)
            .build()
      }
    }
    service
  }

//  def main(args: Array[String]): Unit = {
//    val driveConnect = new GDriveConnect()
//    driveConnect.searchContent("curl")
//  }
}

class GDriveConnect {

  import GDriveConnect._

  def searchContent(text: String, pageSize: Int = 10): Seq[Document] = {

    // Print the names and IDs for up to 10 files.
    val result = getService().files.list
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
      import scala.collection.JavaConverters
      import com.google.api.services.drive.model.File
      //      for (file <- files) {
      //        System.out.printf("%s \n", file)
      //      }
      val sequence: Seq[File] = JavaConverters.asScalaIteratorConverter(files.iterator()).asScala.toSeq
      sequence.map(f => new Document(filename = f.getName, id = f.getId))
    }
  }


}
