package com.vicaya.elasticsearch.dao

import java.nio.charset.StandardCharsets

import org.elasticsearch.action.ActionListener
import org.elasticsearch.action.ingest.PutPipelineRequest
import org.elasticsearch.action.support.master.AcknowledgedResponse
import org.elasticsearch.client.{RequestOptions, RestHighLevelClient}
import org.elasticsearch.common.bytes.BytesArray
import org.elasticsearch.common.unit.TimeValue
import org.elasticsearch.common.xcontent.XContentType

abstract class Publisher(client: RestHighLevelClient) {
  val index: String
  val source: String
  def builder(id: String, json: BytesArray): PutPipelineRequest = {
    new PutPipelineRequest(
      id,
      json,
      XContentType.JSON
    )
      .timeout(TimeValue.timeValueMinutes(2))
  }
  def toByteArray(json: String): BytesArray = {
    new BytesArray(json.getBytes(StandardCharsets.UTF_8))
  }
  def write(request: PutPipelineRequest): Boolean = {
    val listener: ActionListener[AcknowledgedResponse] = new ActionListener[AcknowledgedResponse] {
      override def onResponse(response: AcknowledgedResponse): Unit = {
        println(s"${response}")
        true
      }
      override def onFailure(e: Exception): Unit = {
        println(s"$e")
        false
      }
    }
    client.ingest().putPipelineAsync(request, RequestOptions.DEFAULT, listener)
    true
  }
  def publish(id: String, json: String): Boolean = {
    write(builder(id, toByteArray(json)))
  }
}

class DropBoxPublisher(client: RestHighLevelClient) extends Publisher(client) {
  override val index: String = "dropbox"
  override val source: String = "dropbox"
}

class BoxPublisher(client: RestHighLevelClient) extends Publisher(client) {
  override val index: String = "box"
  override val source: String = "box"
}

class ConfluentPublisher(client: RestHighLevelClient) extends Publisher(client) {
  override val index: String = "confluent"
  override val source: String = "confluent"
}

class GitHubPublisher(client: RestHighLevelClient) extends Publisher(client) {
  override val index: String = "github"
  override val source: String = "github"
}
