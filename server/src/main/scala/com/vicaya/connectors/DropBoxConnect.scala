package com.vicaya.connectors

import java.io.{BufferedOutputStream, FileOutputStream}

import com.dropbox.core.DbxRequestConfig
import com.dropbox.core.oauth.DbxCredential
import com.dropbox.core.util.IOUtil
import com.dropbox.core.v2.DbxClientV2
import com.dropbox.core.v2.files._
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.vicaya.elasticsearch.dao.DropBoxPublisher
import org.apache.kafka.clients.producer.KafkaProducer
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.JavaConverters._

object DropBoxConnect {
  val Token: String = "sl.AgdeieEOf3ZTfS_3SUAQTxK93lDfLjdS_o-6pWR-VXUWGx6LQ0PP53ML4Xiwq75n0CqXQcajgrdNL7CwaNhMwZWf79FCddc2qDn4B4NkZZ7pgmLBHMefVERBK0m5Lmr5lek7odBh"
  val clientId: String = "28kbzjcyr48unom"
  def init(): DbxClientV2 = {
    val dbxCredentials: DbxCredential = new DbxCredential(Token)
    val dbxRequestConfig: DbxRequestConfig = new DbxRequestConfig(clientId)
    val dbxClient = new DbxClientV2(dbxRequestConfig, dbxCredentials)
    dbxClient
  }

  def validate(): Boolean = {
    val client = init()
    client.account() != null
  }

  def download(client: DbxClientV2, path: String, outputFileName: String): Boolean = {
    try {
      val out = new BufferedOutputStream(new FileOutputStream(outputFileName))
      client.files.download(path).download(out, new IOUtil.ProgressListener {
        override def onProgress(l: Long): Unit = {
          println(s"Printing....$l")
        }
      })
      true
    } catch {
      case e: Exception =>
        println(e)
        false
    } finally {
      true
    }
  }

  def search(client: DbxClientV2, query: String): Seq[SearchMatchV2] = {
    //client.files.searchV2(query)
    val builder = client.files.searchV2Builder(query)
    builder.withOptions(
      SearchOptions.
        newBuilder().
        withFilenameOnly(true).
        withFileStatus(FileStatus.ACTIVE).
        withFileExtensions(Seq("pdf", "png", "json").asJava)
        build())
      .start().getMatches.asScala
  }

  def apply(publisher: DropBoxPublisher, mapper: ObjectMapper,kafkaProducer:KafkaProducer[String, Array[Byte]]): DropBoxConnect = {
    new DropBoxConnect(init(), mapper, publisher, kafkaProducer)
  }

  def apply(client: DbxClientV2, mapper: ObjectMapper, publisher: DropBoxPublisher,kafkaProducer:KafkaProducer[String, Array[Byte]]): DropBoxConnect = {
    new DropBoxConnect(client, mapper, publisher, kafkaProducer)
  }
}

class DropBoxConnect(client: DbxClientV2, mapper: ObjectMapper, publisher: DropBoxPublisher, kafkaProducer:KafkaProducer[String, Array[Byte]]) {
  val logger: Logger = LoggerFactory.getLogger("DropBoxConnect")

  def crawlFiles(client: DbxClientV2, path: String = "", list : Seq[Metadata]): Seq[DBMetadata] = {
    val results: ListFolderResult = client.files.listFolder(path)
    results.getEntries.asScala.flatMap(result => {
      val metadata: Metadata = client.files.getMetadata(result.getPathLower)
      val dbMeta: DBMetadata = parse(metadata.toString)
      if (isFolder(dbMeta)) {
        crawlFiles(client, metadata.getPathLower, list)
      } else {
        if (isFileDownloadable(dbMeta)) {
          Seq(metadata)
        } else Seq.empty[Metadata]
      }
    })
    Seq.empty[DBMetadata]
  }

  def isFolder(meta: DBMetadata): Boolean = {
    val tagValue = meta.tag
    val isFolder = tagValue.equalsIgnoreCase("folder")
    isFolder
  }

  def isFileDownloadable(meta: DBMetadata): Boolean = meta.is_downloadable

  def parse(json: String): DBMetadata = {
    mapper.readValue(json, classOf[DBMetadata])
  }

  def startCrawler(): Seq[DBMetadata] = {
    val fileMetadata = crawlFiles(client, "", Seq.empty[Metadata])
    fileMetadata
  }

  def publish(metas: Seq[DBMetadata]): Unit = {
    metas.map(meta => {
      val id: String = meta.id //compact(render(json \ "id"))
      val json: String = mapper.writeValueAsString(meta)
      publisher.publish(id, json)
    })
  }

  def crawlThenPublish(): Boolean = {
    val metas: Seq[DBMetadata] = startCrawler()
    if (metas != null && metas.nonEmpty) {
      logger.info(s"Crawling done.., Found ${metas.size} files to publish to ES")
      publish(metas)
    }
    true
  }

//  def crawlThenDownload(): Boolean = {
//    val metas: Seq[DBMetadata] = startCrawler()
//    if (metas != null && metas.nonEmpty) {
//      logger.info(s"Crawling done.., Found ${metas.size} files to publish to ES")
//      download(metas)
//    }
//    true
//  }

//  def download(metas: Seq[DBMetadata]): Boolean = {
//    metas.map(meta => {
//      client.files().downloadBuilder("./").download()
//    })
//  }
}

case class DBMetadata (
      @JsonProperty(".tag") tag: String,
      name: String,
      id: String,
      client_modified: String,
      server_modified: String,
      rev: String,
      size: Int,
      path_lower: String,
      path_display: String,
      sharing_info: DBSharingInfo,
      is_downloadable: Boolean,
      property_groups: List[DBPropertyGroups],
      has_explicit_shared_members: String,
      content_hash: String,
      file_lock_info: FileLockInfo
)

case class DBSharingInfo(
     read_only: Boolean,
     parent_shared_folder_id: String,
     modified_by: String
)

case class DBPropertyGroups(
     template_id: String,
     fields: List[Map[String, String]]
)

case class FileLockInfo(
      is_lockholder: String,
      lockholder_name: String,
      created: String
)
/*
{
    ".tag": "file",
    "name": "Prime_Numbers.txt",
    "id": "id:a4ayc_80_OEAAAAAAAAAXw",
    "client_modified": "2015-05-12T15:50:38Z",
    "server_modified": "2015-05-12T15:50:38Z",
    "rev": "a1c10ce0dd78",
    "size": 7212,
    "path_lower": "/homework/math/prime_numbers.txt",
    "path_display": "/Homework/math/Prime_Numbers.txt",
    "sharing_info": {
        "read_only": true,
        "parent_shared_folder_id": "84528192421",
        "modified_by": "dbid:AAH4f99T0taONIb-OurWxbNQ6ywGRopQngc"
    },
    "is_downloadable": true,
    "property_groups": [
        {
            "template_id": "ptid:1a5n2i6d3OYEAAAAAAAAAYa",
            "fields": [
                {
                    "name": "Security Policy",
                    "value": "Confidential"
                }
            ]
        }
    ],
    "has_explicit_shared_members": false,
    "content_hash": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
    "file_lock_info": {
        "is_lockholder": true,
        "lockholder_name": "Imaginary User",
        "created": "2015-05-12T15:50:38Z"
    }
}
 */
