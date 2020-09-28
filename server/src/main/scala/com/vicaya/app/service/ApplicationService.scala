package com.vicaya.app.service

import java.io.{File, FileNotFoundException, IOException, InputStreamReader}
import java.util.Collections

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
import com.vicaya.app.configuration.{DatabaseConfig, WorkSpaceCrawlerConfiguration}
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
import com.fasterxml.jackson.annotation.JsonFormat
import org.apache.http.auth.{AuthScope, UsernamePasswordCredentials}
import org.apache.http.impl.client.BasicCredentialsProvider
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder
import org.elasticsearch.client.RestClientBuilder.HttpClientConfigCallback
import org.kohsuke.github.GitHubBuilder

import scala.util.{Failure, Success, Try}

object ApplicationService extends ScalaApplication[WorkSpaceCrawlerConfiguration] {

  val logger: Logger = LoggerFactory.getLogger("com.work.space.crawler.CrawlerService")

  final val HttpClientName: String = "workspace-crawler-http"
  final val databaseName: String = "vicaya"
  final val SQL_PATH_PREFIX: String = "/db/V1_Initial_Schema_Create.sql"

  override def init(bootstrap: Bootstrap[WorkSpaceCrawlerConfiguration]): Unit = {
    super.init(bootstrap)
  }

  override def run(conf: WorkSpaceCrawlerConfiguration, env: Environment): Unit = {

    val ctx: PostgresJdbcContext[SnakeCase] with ImplicitQuery = dbConnect(conf)
    val esClient: RestHighLevelClient = initElasticsearch()

    // Init resource class
    val httpClient = asyncHttpClient()
    val mapper = new ObjectMapper()
    mapper.registerModule(DefaultScalaModule)
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    // Init Service class
    val dropboxConnector: DropBoxConnect = DropBoxConnect(new DropBoxPublisher(esClient), mapper)
    val client =  new BoxAPIConnection(BoxConnect.ClientId, BoxConnect.ClientSecret, BoxConnect.Token, null)

    val boxConnector: BoxConnect = BoxConnect(httpClient, mapper, client, new BoxPublisher(esClient))
    val gitHubClient = new GitHubBuilder().withOAuthToken(GitHubConnect.Token).build
    val gitHubConnector: GitHubConnect = GitHubConnect(gitHubClient, mapper, new GitHubPublisher(esClient))

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

  def initElasticsearch(): RestHighLevelClient = {

    val credentialsProvider = new BasicCredentialsProvider()
    credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials("enterprise_search", "pcDrxUHDBPafJOcPf3BZ"))

    val client: RestHighLevelClient = new RestHighLevelClient(
      RestClient.builder(
        new HttpHost("localhost", 9200, "http"))
        .setHttpClientConfigCallback(new HttpClientConfigCallback() {
          override def customizeHttpClient(httpAsyncClientBuilder: HttpAsyncClientBuilder): HttpAsyncClientBuilder = {
             httpAsyncClientBuilder.setDefaultCredentialsProvider(credentialsProvider)
          }
        })
    )

    client
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
