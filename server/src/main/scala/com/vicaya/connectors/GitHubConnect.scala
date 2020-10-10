package com.vicaya.connectors

import com.fasterxml.jackson.databind.ObjectMapper
import com.vicaya.elasticsearch.dao.GitHubPublisher
import org.kohsuke.github.{GHRepository, GitHub}
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

    //import io.circe.{ Decoder, Encoder, HCursor, Json }
    //implicit val encodeGitHub: Encoder[GitHubRepository] = Encoder[GitHubRepository]

    def publish(metas: Seq[GHRepository]): Unit = {
      metas.foreach(meta => {
        val id: String = meta.getId.toString
        val gitMeta = toGHRepositoryMetadata(meta)
        val str: String = mapper.writeValueAsString(gitMeta)
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

    def toGHRepositoryMetadata(metadata: org.kohsuke.github.GHRepository): GitHubRepository = {
      GitHubRepository (
          metadata.getNodeId,
          metadata.getDescription,
          metadata.getHomepage,
          metadata.getName,
          metadata.getFullName,
          metadata.getHtmlUrl.toString,
          toGitLicense(metadata),
          metadata.getUrl.toString,
          metadata.getSshUrl,
          null,
          metadata.getSvnUrl,
          metadata.getMirrorUrl,
          GitUser(
            metadata.getOwner.getUrl.toString,
            metadata.getOwner.getAvatarUrl,
            metadata.getOwner.getGravatarId,
            metadata.getOwner.getId.toInt,
            metadata.getOwner.getLogin
          ),
          metadata.hasIssues,
          metadata.hasWiki,
          metadata.isFork,
          metadata.hasDownloads,
          metadata.hasPages,
          metadata.isArchived,
          metadata.hasProjects,
          metadata.isAllowSquashMerge,
          metadata.isAllowMergeCommit,
          metadata.isAllowRebaseMerge,
          metadata.isDeleteBranchOnMerge,
          metadata.getForksCount,
          metadata.getStargazersCount,
          metadata.getWatchersCount,
          metadata.getSize,
          metadata.getOpenIssueCount,
          metadata.getSubscribersCount,
          metadata.getPushedAt.toInstant.toString,
          metadata.getDefaultBranch,
          metadata.getLanguage,
          null,
          null,
          null,
          metadata.isTemplate
      )
    }

    def toGitLicense(metadata: org.kohsuke.github.GHRepository): GitLicense = {
      val license = metadata.getLicense
      if (license != null) {
        GitLicense(
          license.getKey,
          license.getName,
          license.isFeatured,
          null,
          license.getDescription,
          license.getCategory,
          license.getImplementation,
          license.getBody,
          if (license.getRequired.nonEmpty) license.getRequired.toSeq else Seq.empty[String],
          if (license.getPermitted.nonEmpty) license.getPermitted.toSeq else Seq.empty[String],
          if (license.getForbidden.nonEmpty) license.getForbidden.toSeq else Seq.empty[String]
        )
      } else {
        null
      }
    }

}
////Name: [connectors] Description: [program to connect with data sources] Languages: [Python]
////GHRepository@1f709068[nodeId=MDEwOlJlcG9zaXRvcnkyNjg3MjQwMTM=,description=program to connect with data sources,homepage=<null>,name=connectors,license=<null>,fork=false,archived=false,size=9,milestones={},language=Python,
//// commits={},source=<null>,parent=<null>,isTemplate=<null>,url=https://api.github.com/repos/WorkplaceXYZ/connectors,id=268724013,nodeId=<null>,createdAt=2020-06-02T06:52:41Z,updatedAt=2020-09-29T05:53:59Z]
case class GitHubMeta(
    name: String,
    description: String,
    languages: Seq[String],
    repository: Seq[GHRepository]
)

case class GitHubRepository(
    nodeId: String,
    description: String,
    homepage: String,
    name: String,
    fullName: String,
    htmlUrl: String,
    license: GitLicense,
    gitUrl: String,
    sshUrl: String,
    cloneUrl: String,
    svnUrl: String,
    mirrorUrl: String,
    owner: GitUser,
    hasIssues: Boolean,
    hasWiki: Boolean,
    fork: Boolean,
    hasDownloads: Boolean,
    hasPages: Boolean,
    archived: Boolean,
    hasProjects: Boolean,
    allowSquashMerge: Boolean,
    allowMergeCommit: Boolean,
    allowRebaseMerge: Boolean,
    deleteBranchOnMerge: Boolean,
    forksCount: Int,
    startgazersCount: Int,
    watchersCount: Int,
    size: Int,
    openIssuesCount: Int,
    subscriberCount: Int,
    pushedAt: String,
    defaultBranch: String,
    language: String,
    permissions: GitHubRepository,
    source: GitHubRepository,
    parent: GitHubRepository,
    isTemplate: Boolean
)

case class GitRepoPermission(
    pull: Boolean,
    push: Boolean,
    admin: Boolean
)

case class GitLicense(
    key: String,
    name: String,
    featured: Boolean,
    htmlUrl: String,
    description: String,
    category: String,
    implementation: String,
    body: String,
    required: Seq[String],
    permitted: Seq[String],
    forbidden: Seq[String]
)

case class GitStats(
    total: Int,
    additions: Int,
    deletions: Int
)

case class GitFile(
    status: String,
    changes: Int,
    additions: Int,
    deletions: Int,
    rawUrl: String,
    blobUrl: String,
    sha: String,
    patch: String,
    fileName: String,
    previousFileName: String
)

case class GitParent(
    url: String,
    sha: String
)

case class GitUser(
    url: String,
    avatar_url: String,
    gravatar_id: String,
    id: Int,
    login: String
)