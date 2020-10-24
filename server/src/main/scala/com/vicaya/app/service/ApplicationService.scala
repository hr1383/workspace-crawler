package com.vicaya.app.service

import java.io.{File, FileNotFoundException, IOException, InputStreamReader}
import java.util.{Collections, Properties}

import com.box.sdk.BoxAPIConnection
import com.datasift.dropwizard.scala.ScalaApplication
import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver
import com.google.api.client.googleapis.auth.oauth2.{GoogleAuthorizationCodeFlow, GoogleClientSecrets}
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.util.store.FileDataStoreFactory
import com.google.api.services.drive.{Drive, DriveScopes}
import com.opentable.db.postgres.embedded.EmbeddedPostgres
import com.vicaya.app.configuration.{DatabaseConfig, KafkaProducerConfiguration, WorkSpaceCrawlerConfiguration}
import com.vicaya.app.resources._
import com.vicaya.database.dao.service.BaseDaoService
import com.vicaya.database.rest.service.UserResource
import com.vicaya.common.util.VicayaUtils
import com.vicaya.connectors.{BoxConnect, DropBoxConnect, GitHubConnect}
import com.vicaya.elasticsearch.dao.{BoxPublisher, DropBoxPublisher, GitHubPublisher}
import io.dropwizard.setup.{Bootstrap, Environment}
import org.slf4j.{Logger, LoggerFactory}
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import io.getquill.{ImplicitQuery, PostgresJdbcContext, SnakeCase}
import org.asynchttpclient.Dsl._
import org.elasticsearch.client.{ElasticsearchClient, RestClient, RestHighLevelClient}
import org.apache.http.HttpHost
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.http.auth.{AuthScope, UsernamePasswordCredentials}
import org.apache.http.impl.client.BasicCredentialsProvider
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder
import org.apache.kafka.common.serialization.{ByteArraySerializer, StringSerializer}
import org.elasticsearch.client.RestClientBuilder.HttpClientConfigCallback
import org.kohsuke.github.GitHubBuilder
import software.amazon.awssdk.auth.credentials.InstanceProfileCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import com.box.sdk.BoxTransactionalAPIConnection

import scala.util.{Failure, Success, Try}

object ApplicationService extends ScalaApplication[WorkSpaceCrawlerConfiguration] {

  val logger: Logger = LoggerFactory.getLogger("com.work.space.crawler.CrawlerService")

  final val HttpClientName: String = "workspace-crawler-http"
  final val databaseName: String = "vicayah"
  final val SQL_PATH_PREFIX: String = "/db/V1_Initial_Schema_Create.sql"

  override def init(bootstrap: Bootstrap[WorkSpaceCrawlerConfiguration]): Unit = {
    super.init(bootstrap)
  }

  override def run(conf: WorkSpaceCrawlerConfiguration, env: Environment): Unit = {

    val ctx: PostgresJdbcContext[SnakeCase] with ImplicitQuery = dbConnect(conf)
    val esClient: RestHighLevelClient = initElasticsearch(conf)

    // Init Kafka
    val kafkaProducer:KafkaProducer[String, Array[Byte]] = initKafkaProducer(conf.kafkaProducerConfiguration)

    // Init AWS S3
    val s3client: S3Client = initAWSs3Client()

    // Init resource class
    val httpClient = asyncHttpClient()
    val mapper = new ObjectMapper()
    mapper.registerModule(DefaultScalaModule)
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    // Init Service class
    val dropboxConnector: DropBoxConnect = DropBoxConnect(new DropBoxPublisher(esClient), mapper, kafkaProducer, s3client)
    val api = new BoxTransactionalAPIConnection(BoxConnect.primaryToken)
    val client =  new BoxAPIConnection(api.getClientID, api.getClientSecret, api.getAccessToken, api.getRefreshToken)

    val boxConnector: BoxConnect = BoxConnect(httpClient, mapper, client, new BoxPublisher(esClient), kafkaProducer, s3client)
    val gitHubClient = new GitHubBuilder().withOAuthToken(GitHubConnect.Token).build
    val gitHubConnector: GitHubConnect = GitHubConnect(gitHubClient, mapper, new GitHubPublisher(esClient), kafkaProducer, s3client)

    env.jersey().register(new UserResource(new BaseDaoService(ctx)))
//    env.jersey().register(new ServiceResource(new ConnectorService(
//      new GDriveConnect(getGService()),
//      new ConfluenceConnect(httpClient, mapper),
//      new JiraConnect(httpClient, mapper),
//      new BoxConnect(httpClient, mapper))))
    env.jersey().register(new DropBoxResource(dropboxConnector))
    env.jersey().register(new BoxResource(boxConnector))
    env.jersey().register(new GitHubResource(gitHubConnector))
  }

  private def dbConnect(conf: WorkSpaceCrawlerConfiguration) = {
    val dbConfig: DatabaseConfig = conf.databaseConfig
    // Init Quill context
    val pgDataSource = new org.postgresql.ds.PGSimpleDataSource()
    if (dbConfig.embeddedMode) {
      startEmbeddedDatabase()
      pgDataSource.setUser("postgres")
    } else {
      pgDataSource.setURL(dbConfig.url)
      pgDataSource.setUser(dbConfig.user)
      pgDataSource.setPassword(dbConfig.password)
      pgDataSource.setDatabaseName(databaseName)
      pgDataSource.setConnectTimeout(dbConfig.maxWaitForConnection)
    }

    // Init HikariConfig
    val config = new HikariConfig()
    config.setDataSource(pgDataSource)

    // Create a datasource
    val ds = new HikariDataSource(config)
    val ctx = new PostgresJdbcContext[SnakeCase](SnakeCase, ds) with ImplicitQuery

    // Init Database
    Try(ds.getConnection) match {
      case Success(_) =>
        logger.info(s"Successfully connected to Postgres ${ds.getJdbcUrl}")
        createDatabaseSchema(ds.getConnection, dbConfig.sqlFileLocation)
      case Failure(exception) =>
        logger.info(s"Failed to connected to Postgres ${ds.getJdbcUrl}", exception)
    }
    ctx
  }

  def startEmbeddedDatabase(): EmbeddedPostgres = {
    val server = EmbeddedPostgres.builder().setPort(5432).start()
    server
  }

  def createDatabaseSchema(connection: java.sql.Connection, fileLocation: String): Unit = {
    val statement = connection.createStatement()
    //val sqlFile = getClass.getResource(fileLocation)
    println(s"Running sql file $fileLocation")
    statement.execute(VicayaUtils.readFileContent(fileLocation))
  }

  def initElasticsearch(conf: WorkSpaceCrawlerConfiguration): RestHighLevelClient = {

    val credentialsProvider = new BasicCredentialsProvider()
    credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials("enterprise_search", "pcDrxUHDBPafJOcPf3BZ"))

    val client: RestHighLevelClient = new RestHighLevelClient(
      RestClient.builder(
        new HttpHost(conf.elasticSearchConfig.hostname, conf.elasticSearchConfig.port, "http"))
        .setHttpClientConfigCallback(new HttpClientConfigCallback() {
          override def customizeHttpClient(httpAsyncClientBuilder: HttpAsyncClientBuilder): HttpAsyncClientBuilder = {
             httpAsyncClientBuilder.setDefaultCredentialsProvider(credentialsProvider)
          }
        })
    )

    client
  }

  def initKafkaProducer(conf: KafkaProducerConfiguration): KafkaProducer[String, Array[Byte]] = {
    val props = new Properties()
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, conf.bootstrapServers)
    props.put(ProducerConfig.CLIENT_ID_CONFIG, "VicayahProducer")
    props.put(ProducerConfig.RETRIES_CONFIG.toString, conf.retries.toString)
    props.put(ProducerConfig.ACKS_CONFIG, conf.acks)
    props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, conf.compressionType)
    props.put(ProducerConfig.BATCH_SIZE_CONFIG.toString, conf.batchSize.toString)
    props.put(ProducerConfig.LINGER_MS_CONFIG.toString, conf.lingerMs.toString)
    props.put(ProducerConfig.MAX_BLOCK_MS_CONFIG.toString, conf.maxBlockMs.toString)
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer")
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.ByteArraySerializer")
    val producer = new KafkaProducer[String, Array[Byte]](props)
    producer
  }

  def initAWSs3Client(): S3Client = {
    val AWS_ACCESS_KEY_ID = ""
    val SECRET_ACCESS_KEY = ""
    val region: Region = Region.US_WEST_2

//    val provider: BasicCredentialsProvider = new BasicCredentialsProvider()
//      provider.
//      setCredentials(
//        AuthScope.ANY,
//        new UsernamePasswordCredentials(AWS_ACCESS_KEY_ID, SECRET_ACCESS_KEY)
//      )

    val s3Client:S3Client = S3Client.
      builder().
      credentialsProvider(
        InstanceProfileCredentialsProvider.
          builder().
          build()).
      region(region).
      build()
    s3Client
  }

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
  private def getCredentials(HTTP_TRANSPORT: NetHttpTransport): Credential = {
    // Load client secrets.
    val in = classOf[Nothing].getResourceAsStream(CREDENTIALS_FILE_PATH)
    if (in == null) throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH)
    val clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in))
    // Build flow and trigger user authorization request.
    val flow = new GoogleAuthorizationCodeFlow.Builder(HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
      .setDataStoreFactory(new FileDataStoreFactory(new File(TOKENS_DIRECTORY_PATH))).setAccessType("offline").build
    val receiver = new LocalServerReceiver.Builder().setPort(8888).build
    new AuthorizationCodeInstalledApp(flow, receiver).authorize("user")
  }

  def getGService(): Drive = {
    new Drive
    .Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
      .setApplicationName(APPLICATION_NAME)
      .build()
  }

}
