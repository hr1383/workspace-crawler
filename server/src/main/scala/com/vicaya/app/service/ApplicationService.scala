package com.vicaya.app.service

import com.datasift.dropwizard.scala.ScalaApplication
import com.opentable.db.postgres.embedded.EmbeddedPostgres
import com.vicaya.app.configuration.{DatabaseConfig, WorkSpaceCrawlerConfiguration}
import com.vicaya.connectors.{ConnectorService, GDriveConnect}
import com.vicaya.database.dao.service.BaseDaoService
import com.vicaya.database.rest.service.UserResource
import com.vicaya.resources.ServiceResource
import io.dropwizard.setup.{Bootstrap, Environment}
import org.slf4j.{Logger, LoggerFactory}
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import io.getquill.{ImplicitQuery, PostgresJdbcContext, SnakeCase}

import scala.util.{Failure, Success, Try}

object ApplicationService extends ScalaApplication[WorkSpaceCrawlerConfiguration] {

    val logger: Logger = LoggerFactory.getLogger("com.work.space.crawler.CrawlerService")

    final val HttpClientName: String = "workspace-crawler-http"
    final val databaseName: String = "vicaya"

    override def init(bootstrap: Bootstrap[WorkSpaceCrawlerConfiguration]): Unit = {
        super.init(bootstrap)
    }

    override def run(conf: WorkSpaceCrawlerConfiguration, env: Environment): Unit = {

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
//        val ds = new HikariDataSource(config)
//        val ctx = new PostgresJdbcContext[SnakeCase](SnakeCase, ds) with ImplicitQuery
//
//        // Init Database
//        Try(ds.getConnection) match  {
//            case Success(_) =>
//                logger.info(s"Successfully connected to Postgres ${ds.getJdbcUrl}")
//
//            case Failure(exception) =>
//                logger.info(s"Failed to connected to Postgres ${ds.getJdbcUrl}", exception)
//        }
        // Init resource class
        //env.jersey().register(new WorkSpaceResource())
//        env.jersey().register(new UserResource(new BaseDaoService(ctx)))
        env.jersey().register(new ServiceResource(new ConnectorService(new GDriveConnect)))
    }

    def startEmbeddedDatabase(): EmbeddedPostgres = {
        val server = EmbeddedPostgres.builder().setPort(5435).start()
        server
    }

}
