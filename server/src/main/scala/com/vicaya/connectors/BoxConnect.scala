package com.vicaya.connectors

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.vicaya.app.response.{ConnectorEnum, Document}
import org.asynchttpclient.AsyncHttpClient
import org.asynchttpclient.Dsl.asyncHttpClient


object BoxConnect {
  def main(args: Array[String]): Unit = {
    val httpClient = asyncHttpClient()
    val mapper = new ObjectMapper()
    mapper.registerModule(DefaultScalaModule)
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    val driveConnect = new BoxConnect(httpClient, mapper)
//    System.out.println(driveConnect.searchContent("financial"))
    httpClient.close()
  }
}


class BoxConnect(httpClient: AsyncHttpClient, mapper: ObjectMapper) extends SearchConnect {

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
      if (boxResponse.results.length > 0) {
        boxResponse.results.map(result => Document(source = ConnectorEnum.BOX.toString,
          url = LINK_URL + s"/${result.file_type}/" + result.id,
          title = result.name,
          description = result.description))
      } else Seq.empty
    }
  }
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
                     modified_by: BoxUser
                    )
case class BoxUser(
                  @JsonProperty("type") category: String,
                  id: String,
                  name: String,
                  email: String
                  )