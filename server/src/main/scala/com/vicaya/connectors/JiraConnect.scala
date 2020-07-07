package com.vicaya.connectors

import com.fasterxml.jackson.annotation.{JsonIgnoreProperties, JsonProperty}
import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.vicaya.app.response.{ConnectorEnum, Document}
import org.apache.http.auth.UsernamePasswordCredentials
import org.asynchttpclient.Dsl.asyncHttpClient
import org.asynchttpclient.{AsyncHttpClient, Realm}
import org.slf4j.{Logger, LoggerFactory}


object JiraConnect {
  val logger: Logger = LoggerFactory.getLogger("com.vicaya.connectors.JiraConnect")
  def main(args: Array[String]): Unit = {
    val httpClient = asyncHttpClient()
    val mapper = new ObjectMapper()
    mapper.registerModule(DefaultScalaModule)
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    val driveConnect = new JiraConnect(httpClient, mapper)
    logger.info(s"${driveConnect.searchContent("body")}")
    httpClient.close()
  }
}


class JiraConnect(httpClient: AsyncHttpClient, mapper: ObjectMapper) extends SearchConnect {
  import JiraConnect._
  val PARENT_URL = "https://workplace-xyz.atlassian.net"
  val SEARCH_API = "/rest/api/2/search?"
  val email = "hrsht.rastogi13@gmail.com"
  val authToken = "xxx"
  val creds = new UsernamePasswordCredentials(email, authToken)

  override def searchContent(text: String, pageSize: Int): Seq[Document] = {
    val requestUrl = PARENT_URL + SEARCH_API + "jql=text~" + text
    System.out.println(requestUrl)
    val httpGet = httpClient.prepareGet(requestUrl)
    val realm = new Realm.Builder(email, authToken)
      .setUsePreemptiveAuth(true)
      .setScheme(Realm.AuthScheme.BASIC)
      .build();
    //todo make it async
    val response = httpGet.setRealm(realm).execute().get()
    logger.info("Response " + response.getStatusCode)
    logger.info("Response body " + response.getResponseBody)
    if (response.getStatusCode != 200) {
      Seq.empty
    } else {
      val confResponse = mapper.readValue(response.getResponseBody, classOf[JiraResponse])
      if (confResponse.results.length > 0) {
        confResponse.results.map(result => Document(source = ConnectorEnum.JIRA.toString,
          url = PARENT_URL + "/browse/" + result.key,
          title = result.properties.summary,
          description = result.properties.description))
      } else Seq.empty
    }
  }
}

case class JiraResponse(
                         @JsonProperty("issues") results: List[JiraResult],
                         @JsonProperty("startAt") start: Int,
                         @JsonProperty("maxResults") limit: Int,
                         @JsonProperty("total") size: Int)

case class JiraResult(id: String,
                      key: String,
                      @JsonProperty("fields") properties: JiraProperties)

case class JiraProperties(issueType: JiraIssueType,
                          project: JiraProject,
                          watches: JiraWatches,
                          priority: JiraPriority,
                          assignee: JiraPerson,
                          stats: JiraStatus,
                          summary: String,
                          description: String,
                          creator: JiraPerson,
                          reporter: JiraPerson)

case class JiraIssueType(name: String)
case class JiraProject(key: String, id: String, name: String)
case class JiraWatches(watchCount: Int)
case class JiraPriority(id: String, name: String)
case class JiraStatus(name: String)
case class JiraPerson(emailIdAddress: String, accountId: String)

