package com.vicaya.elasticsearch.dao

import java.nio.charset.StandardCharsets

import org.elasticsearch.action.ActionListener
import org.elasticsearch.action.index.{IndexRequest, IndexResponse}
import org.elasticsearch.action.ingest.PutPipelineRequest
import org.elasticsearch.action.support.master.AcknowledgedResponse
import org.elasticsearch.client.indices.{CreateIndexRequest, CreateIndexResponse, GetIndexRequest}
import org.elasticsearch.client.{RequestOptions, RestHighLevelClient}
import org.elasticsearch.common.bytes.BytesArray
import org.elasticsearch.common.unit.TimeValue
import org.elasticsearch.common.xcontent.XContentType

abstract class Publisher(client: RestHighLevelClient) {
  val index: String
  val source: String
  def builder(id: String, json: BytesArray): IndexRequest = {
    if (!indexExists(index)) {
      createIndex(index)
    }
    new IndexRequest(index).id(id).source(json, XContentType.JSON)
    .timeout(TimeValue.timeValueMinutes(2))
  }
  def toByteArray(json: String): BytesArray = {
    new BytesArray(json.getBytes(StandardCharsets.UTF_8))
  }
  def createIndex(index: String): CreateIndexResponse = {
    val request = new CreateIndexRequest(index)
    client.indices().create(request, RequestOptions.DEFAULT)
  }

  def indexExists(index: String):Boolean = {
    client.indices().exists(new GetIndexRequest(index), RequestOptions.DEFAULT)
  }
  def write(request: IndexRequest): Boolean = {
    val listener: ActionListener[IndexResponse] = new ActionListener[IndexResponse] {
      override def onResponse(response: IndexResponse): Unit = {
        println(s"${response}")
        true
      }
      override def onFailure(e: Exception): Unit = {
        println(s"$e")
        false
      }
    }
    client.indexAsync(request, RequestOptions.DEFAULT, listener)
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
