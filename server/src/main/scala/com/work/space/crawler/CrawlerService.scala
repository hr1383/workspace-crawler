package com.work.space.crawler

import com.datasift.dropwizard.scala.ScalaApplication
import com.work.space.crawler.configuration.WorkSpaceCrawlerConfiguration
import com.work.space.crawler.resources.WorkSpaceResource
import io.dropwizard.setup.{Bootstrap, Environment}
import org.slf4j.{Logger, LoggerFactory}

object CrawlerService extends ScalaApplication[WorkSpaceCrawlerConfiguration] {

  val logger: Logger = LoggerFactory.getLogger("com.work.space.crawler.CrawlerService")

  final val HttpClientName: String = "workspace-crawler-http"

  override def init(bootstrap: Bootstrap[WorkSpaceCrawlerConfiguration]): Unit = {
    super.init(bootstrap)
  }
  override def run(conf: WorkSpaceCrawlerConfiguration, env: Environment): Unit = {
    env.jersey().register(new WorkSpaceResource())
  }

}
