package com.vicaya.app.resources

import com.vicaya.connectors.GitHubConnect
import javax.ws.rs.core.MediaType
import javax.ws.rs.{Consumes, POST, Path, Produces}

object GitHubResource {

}

@Path("/v1/github")
class GitHubResource(githubConnect: GitHubConnect) {

  // curl -XPOST http://localhost:8080/v1/github/crawl -H 'Content-Type: application/json' -H "Accept: application/json"
  @POST
  @Path("/crawl")
  @Consumes(Array(MediaType.APPLICATION_JSON))
  @Produces(Array(MediaType.APPLICATION_JSON))
  def crawl(): Boolean = {
    githubConnect.crawlThenPublish()
  }
}
