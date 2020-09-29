package com.vicaya.connectors

import com.box.sdk.{BoxFolder, BoxItem}
import com.fasterxml.jackson.databind.ObjectMapper
import com.vicaya.elasticsearch.dao.{BoxPublisher, GitHubPublisher}
import org.kohsuke.github.{GHRepository, GitHub, GitHubBuilder}
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.JavaConversions._

object GitHubConnect {
  val Token: String = ""
  val organizationName: String = "WorkplaceXYZ"

  def apply(client: GitHub,  mapper: ObjectMapper, publisher: GitHubPublisher): GitHubConnect = {
    new GitHubConnect(client, mapper, publisher)
  }
}

class GitHubConnect(client: GitHub, mapper: ObjectMapper, publisher: GitHubPublisher) {
  import GitHubConnect._
  val logger: Logger = LoggerFactory.getLogger("GitHubConnect")

  def crawler(): Seq[GHRepository] = {
    client.getOrganization(organizationName).getRepositories.map(repo => {
      println(s"Name: [${repo._1}] Description: [${repo._2.getDescription}] Languages: [${repo._2.getLanguage}]")
      println(s"${repo._2.toString}")
      repo._2
    }).toSeq
  }

  def publish(metas: Seq[GHRepository]): Unit = {
    metas.foreach(meta => {
      val id: String = meta.getId.toString
      val str: String = mapper.writeValueAsString(meta)
      publisher.publish(id, str)
    })
  }

  def crawlThenPublish(): Boolean = {
    val metas: Seq[GHRepository] = crawler()
    if (metas != null && metas.nonEmpty) {
      logger.info(s"Crawling done.., Found ${metas.size} files to publish to ES")
      publish(metas)
    }
    true
  }

}
