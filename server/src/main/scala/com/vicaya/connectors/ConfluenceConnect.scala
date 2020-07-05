package com.vicaya.connectors

import com.fasterxml.jackson.annotation.{JsonIgnoreProperties, JsonProperty}
import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.vicaya.app.response.{ConnectorEnum, Document}
import org.apache.http.auth.UsernamePasswordCredentials
import org.apache.http.impl.auth.BasicScheme
import org.asynchttpclient.Dsl.asyncHttpClient
import org.asynchttpclient.{AsyncHttpClient, ListenableFuture, Realm, Response}
import java.util.concurrent.{CompletableFuture, Future}

object ConfluenceConnect {
  def main(args: Array[String]): Unit = {
    val httpClient = asyncHttpClient()
    val mapper = new ObjectMapper()
    mapper.registerModule(DefaultScalaModule)
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    val driveConnect = new ConfluenceConnect(httpClient, mapper)
    System.out.println(driveConnect.searchContent("body"))
    httpClient.close()
  }
}

class ConfluenceConnect(httpClient: AsyncHttpClient, mapper: ObjectMapper) extends SearchConnect {

  val PARENT_URL = "https://workplace-xyz.atlassian.net"
  val SEARCH_API = "/wiki/rest/api/content/search?"
  val email = "hrsht.rastogi13@gmail.com"
  val authToken = "xxxx"
  val creds = new UsernamePasswordCredentials(email, authToken)

  override def searchContent(text: String, pageSize: Int): Seq[Document] = {
    val requestUrl = PARENT_URL + SEARCH_API + "cql=text~" + text
    System.out.println(requestUrl)
    val httpGet = httpClient.prepareGet(requestUrl)
    val realm = new Realm.Builder(email, authToken)
      .setUsePreemptiveAuth(true)
      .setScheme(Realm.AuthScheme.BASIC)
      .build();
    //todo make it async
    val response = httpGet.setRealm(realm).execute().get()
    System.out.println("Response " + response.getStatusCode)
    System.out.println("Response body " + response.getResponseBody)
    if (response.getStatusCode != 200) {
      Seq.empty
    } else {
      val confResponse = mapper.readValue(response.getResponseBody, classOf[ConfluenceResponse])
      if (confResponse.results.length > 0) {
        confResponse.results.map(result => Document(source = ConnectorEnum.CONFLUENCE.toString,
          url = PARENT_URL + result.links.webui,
          title = result.title))
      } else Seq.empty
    }

  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  case class ConfluenceResponse(@JsonProperty("results") results: List[ConfluenceResult],
                                start: Int,
                                limit: Int,
                                size: Int)

  case class ConfluenceResult(id: String,
                              status: String,
                              title: String,
                              @JsonProperty("type") category: String,
                              @JsonProperty("_links") links: Link)

  case class Link(webui: String, self: String, tinyui: String)