package com.vicaya.connectors


import java.io.FileInputStream
import java.sql.Timestamp

import com.box.sdk.{BoxAPIConnection, BoxFile, BoxFolder, BoxItem, ProgressListener}
import com.fasterxml.jackson.annotation.{JsonFormat, JsonProperty}
import com.fasterxml.jackson.databind.ObjectMapper
import com.vicaya.app.response.{ConnectorEnum, Document}
import com.vicaya.elasticsearch.dao.BoxPublisher
import com.vicaya.kafka.pojo.{KafkaFileMetadata, KafkaSerializer}
import org.asynchttpclient.AsyncHttpClient
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.JavaConversions._
import io.circe.generic.auto._
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest

import scala.util.{Failure, Success, Try}

object BoxConnect {
  val Token: String = "AnPzWSBRWaA6Br6KWH8xrV56g6ZtEXv4"
  val ClientId: String = "t99zy30x5a9khx5rhrgz5s0watrl5uup"
  val ClientSecret: String = "pIdZmsbhXosvWIng86rFnybmpDSDdGGW"
  val primaryToken: String = "VqRLRH0ogjfWuUwexD0WE546wgPF7BEa"
  val secondaryToken: String = "F8Mq6q5trIMsAKHTplq0lXZTZMmfFkjF"
  val BUCKET_NAME: String = "com.vicayah.internal.dev.files.backup"


  // primary key: Ww65fizrPNeo4UA4chQIBHuny6dc3ULV
  // Secondary Key: cWDqyWIBAbBQG5kCiamwrZb6eEZN49md

  def apply(httpClient: AsyncHttpClient, mapper: ObjectMapper, client: BoxAPIConnection, publisher: BoxPublisher, kafkaProducer:KafkaProducer[String, Array[Byte]], s3Client:S3Client): BoxConnect = {
    new BoxConnect(httpClient, mapper, client, publisher, kafkaProducer, s3Client)
  }
}


class BoxConnect(httpClient: AsyncHttpClient, mapper: ObjectMapper, client: BoxAPIConnection, publisher: BoxPublisher, kafkaProducer:KafkaProducer[String, Array[Byte]], s3Client:S3Client) extends SearchConnect {
  val logger: Logger = LoggerFactory.getLogger("BoxConnect")

  val PARENT_URL = "https://api.box.com/2.0/search"
  val LINK_URL  = "http://app.box.com/"

  override def searchContent(text: String, pageSize: Int): Seq[Document] = {
    val requestUrl = PARENT_URL + "?query=" + text
    val httpGet = httpClient.prepareGet(requestUrl)
    //token expires in an hour
    httpGet.setHeader("Authorization", "Bearer xxxx")
    //todo make it async
    val response = httpGet.execute().get()
    if (response.getStatusCode != 200) {
      Seq.empty
    } else {
      val boxResponse = mapper.readValue(response.getResponseBody, classOf[BoxResponse])
      if (boxResponse.results.nonEmpty) {
        boxResponse.results.map(result => Document(source = ConnectorEnum.BOX.toString,
          url = LINK_URL + s"/${result.file_type}/" + result.id,
          title = result.name,
          description = result.description))
      } else Seq.empty
    }
  }

  def crawlThenPublish(): Boolean = {
    val rootFolder = BoxFolder.getRootFolder(client)
    val metas: Seq[BoxItem#Info] = startCrawler(rootFolder)
    if (metas != null && metas.nonEmpty) {
      logger.info(s"Crawling done.., Found ${metas.size} files to publish to ES")
      publish(metas)
    }
    true
  }

  def crawlThenDownload(): Boolean = {
    val rootFolder = BoxFolder.getRootFolder(client)
    val metas: Seq[BoxItem#Info] = startCrawler(rootFolder)
    if (metas != null && metas.nonEmpty) {
      logger.info(s"Crawling done.., Found ${metas.size} files to publish to ES")
      publish(metas)
      download(metas)
    }
    true
  }

  import io.circe.{ Decoder, Encoder, HCursor, Json }
  implicit val encodeBox: Encoder[BoxFileMetadata] = Encoder[BoxFileMetadata]

  def publish(metas: Seq[BoxItem#Info]): Unit = {
    metas.foreach(meta => {
      val id: String = meta.getID
      val metadata: BoxFileMetadata = toBoxMetadata(meta)
      //val json: Json = metadata.asJson
      val str: String = mapper.writeValueAsString(metadata)
      publisher.publish(id, str)
    })
  }

  import java.io.FileOutputStream
  def download(metas: Seq[BoxItem#Info]): Unit = {
    metas.foreach(meta => {
     val download = Try {
       val file: BoxFile = new BoxFile(client, meta.getID)
       val info: BoxFile#Info = file.getInfo()
       val loc: String = s"/Users/rgupta/Development/vicaya/downloads/${info.getName}"
       val stream = new FileOutputStream(loc)
         file.download(stream, new ProgressListener {
         override def onProgressChanged(l: Long, l1: Long): Unit = {
           logger.info(s"Downloading ${info.getName} PercentageDownloaded: ${(l/l1) * 100}")
           if (l >= l1) {
             logger.info(s"Download complete and can be used as a signal to upload")
             // Upload to s3
//             val s3Loc = s3Client.putObject(
//               PutObjectRequest.
//                 builder().
//                 bucket(BoxConnect.BUCKET_NAME).
//                 key("BOX").
//                 build(),
//               RequestBody.fromInputStream(new FileInputStream(info.getName), 8192)
//             )
           }
         }
       })
       // once downloaded publish to kafka
       val metadata = kafkaProducer
         .send(
           new ProducerRecord[String, Array[Byte]](
             "file-metadata",
             meta.getID,
             KafkaSerializer.serialise(KafkaFileMetadata(
               id = meta.getID,
               name = meta.getName,
               doc_type = meta.getType,
               size = meta.getSize,
               s3_location = loc,
               uuid = meta.getID,
               source = "box",
               last_modified = null
             ))
           )
         ).get()
       logger.info(s"${metadata.partition()}")

     } match {
        case Success(value) =>
            logger.info(s"Successfully downloaded file $value")
          Some(value)
        case Failure(exception) =>
          logger.error("Error while downloading file", exception)
         None
      }
      logger.info(s"Downloaded Info $download")
    })
  }

  def startCrawler(folder: BoxFolder): Seq[BoxItem#Info] = {
    folder.getChildren.flatMap(child => {
      println(s"${child.getName} ${child.getType}")
      if (isFolder(child)) {
        startCrawler(new BoxFolder(client, child.getID))
      } else {
        Seq(child)
      }
    }).toSeq
  }

  def isFolder(item: BoxItem#Info): Boolean = {
    item.getType.equalsIgnoreCase("folder")
  }

  def optional[T](x: T): Option[T] = {
    val t = x match {
      case s: String if s.nonEmpty => s
      case i: Int if i != 0 => i
      case l: Long if l != 0L => l
      case f: Float if f != 0.0d => f
      case d: Double if d != 0.0d => d
      case b: Boolean if b => b
      case e: Enumeration if e.maxId!= 0 => e
      case _ => null
    }
    Option(t.asInstanceOf[T])
  }

  def toSqlTimestamp(date: java.util.Date): Timestamp = {
    if (date != null) {
      Timestamp.from(date.toInstant)
    } else {
      null
    }
  }

  def toBoxMetadata(metadata: BoxItem#Info): BoxFileMetadata = {
    BoxFileMetadata (
     `type` = optional(metadata.getType).orNull,
     sequenceID = metadata.getSequenceID,
     etag = metadata.getEtag,
     name = metadata.getName,
     createdAt = toSqlTimestamp(metadata.getCreatedAt),
     modifiedAt = toSqlTimestamp(metadata.getModifiedAt),
     description = metadata.getDescription,
     size = metadata.getSize,
     pathCollection = null, //toBoxFolderInfo(metadata.getPathCollection.toList),
     createdBy = null, // toBoxUser(metadata.getCreatedBy),
     modifiedBy = null,
     trashedAt = toSqlTimestamp(metadata.getTrashedAt),
     purgedAt = toSqlTimestamp(metadata.getPurgedAt),
     contentCreatedAt = toSqlTimestamp(metadata.getContentCreatedAt),
     contentModifiedAt = toSqlTimestamp(metadata.getContentModifiedAt),
     ownedBy = null,
     sharedLink = null, //toBoxSharedLink(),
     tags = if (metadata.getTags != null && metadata.getTags.nonEmpty) metadata.getTags.toList else Seq.empty[String],
     parent = metadata.getParent,
     itemStatus = metadata.getItemStatus,
     expiresAt = toSqlTimestamp(metadata.getExpiresAt),
     collections = null //metadata.getCollections.map(toBoxCollection).toSet
    )
  }

  def toBoxFolderInfo(collection: List[BoxFolder#Info]): Seq[BoxFolderInfo] = {
    if (collection == null || collection.isEmpty) {
      Seq.empty[BoxFolderInfo]
    } else {
      collection.map(coll => {
        BoxFolderInfo(
          uploadEmail = null, //coll.getUploadEmail,
          hasCollaborations = coll.getHasCollaborations,
          syncState = coll.getSyncState,
          permissions = null, //Set(coll.getPermissions.map(_.toString)),
          canNonOwnersInvite = coll.getCanNonOwnersInvite,
          isWatermarked = coll.getIsWatermarked,
          isCollaborationRestrictedToEnterprise = coll.getIsCollaborationRestrictedToEnterprise,
          isExternallyOwned = coll.getIsExternallyOwned,
          allowedSharedLinkAccessLevels = coll.getAllowedSharedLinkAccessLevels,
          allowedInviteeRoles = coll.getAllowedInviteeRoles,
          classification = toBoxClassification(coll.getClassification)
        )
      })
    }
  }

  def toBoxClassification(boxClassification: com.box.sdk.BoxClassification): VBoxClassification = {
    VBoxClassification(
      color = boxClassification.getColor,
      definition = boxClassification.getDefinition,
      name = boxClassification.getName
    )
  }

//  def toBoxUser(info: BoxUser#Info): BoxUser = {
////    BoxUser (
////      val category: String = createdBy.,
////      val id: String = createdBy.getID,
////      val name: String = createdBy.get,
////      val email: String = createdBy.getEmailAliases
////    )
//  }

//  def toBoxCollection(info: com.box.sdk.BoxCollection#Info): BoxCollection = {
//    BoxCollection(
//      val collectionType: String = info.getCollectionType,
//      val name: String = info.getName
//    )
//  }

}

case class BoxResponse(
  @JsonProperty("total_count") count: Int,
  @JsonProperty("entries") results: Seq[BoxResult])

case class BoxResult(
  @JsonProperty("type") file_type: String,
  id: String,
  name: String,
  description: String,
  created_by: BoxUser,
  modified_by: BoxUser)

case class BoxUser(
  @JsonProperty("type") category: String,
  id: String,
  name: String,
  email: String
)

case class BoxFileMetadata(
  `type`: String = null,
  sequenceID: String = null,
  etag: String = null,
  name: String = null,
  createdAt: Timestamp = null,
  modifiedAt: Timestamp = null,
  description: String = null,
  size: Long = 0L,
  pathCollection: Seq[BoxFolderInfo] = null,
  createdBy: BoxUser = null,
  modifiedBy: BoxUser = null,
  trashedAt: Timestamp = null,
  purgedAt: Timestamp = null,
  contentCreatedAt: Timestamp = null,
  contentModifiedAt: Timestamp = null,
  ownedBy: BoxUser = null,
  sharedLink: BoxSharedLink = null,
  tags: Seq[String] = null,
  parent: BoxFolder#Info = null,
  itemStatus: String = null,
  expiresAt: Timestamp = null,
  collections: Set[BoxCollection] = null
)

case class BoxSharedLink(
  url: String = null,
  downloadUrl: String = null,
  vanityUrl: String = null,
  isPasswordEnabled: Boolean = false,
  password: String = null,
  unsharedAt: Timestamp = null,
  downloadCount: Long = 0L,
  previewCount: Long = 0L,
  access: String = null,
  effectiveAccess: String = null,
  permissions: String = null
)

case class BoxCollection(
  collectionType: String = null,
  name: String = null
)

case class BoxFolderInfo(
  uploadEmail: BoxUploadEmail = null,
  hasCollaborations: Boolean = false,
  syncState: BoxFolder.SyncState = null,
  permissions: Set[String] = null,
  canNonOwnersInvite: Boolean = false,
  isWatermarked: Boolean = false,
  isCollaborationRestrictedToEnterprise: Boolean = false,
  isExternallyOwned: Boolean = false,
  allowedSharedLinkAccessLevels: Seq[String] = null,
  allowedInviteeRoles: Seq[String] = null,
  classification: VBoxClassification = null
)

case class VBoxClassification(
  color: String = null,
  definition: String = null,
  name: String = null
)

case class BoxUploadEmail(
  access: String = null,
  email: String = null
)

