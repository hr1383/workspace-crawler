package com.work.space.crawler.configuration

import com.datasift.dropwizard.scala.validation.constraints._
import com.fasterxml.jackson.annotation.JsonProperty
import io.dropwizard.Configuration


case class WorkSpaceCrawlerConfiguration(
  @JsonProperty("elasticSearch") elasticSearchConfig: ElasticSearchConfig,
  @JsonProperty("httpConfig") httpConfig: HttpConfig,
  @JsonProperty("databaseConfig") databaseConfig: DatabaseConfig
) extends Configuration


case class ElasticSearchConfig(
  @JsonProperty @Min(100) @Max(30000) connectionTtlMs : Int = 5000,
  @JsonProperty maxConnTotal: Int = 20,
  @NotNull @NotEmpty @JsonProperty("url") url: String
)


@SerialVersionUID(100L)
case class HttpConfig(
  @JsonProperty connectionTtlMs: Int,
  @JsonProperty pooledConnectionIdleTimeoutMs: Int,
  @JsonProperty readTimeoutMs: Int,
  @JsonProperty requestTimeoutMs: Int,
  @JsonProperty connectTimeoutMs: Int,
  @JsonProperty socketTimeoutMs: Int,
  @JsonProperty maxRetry: Int,
  @JsonProperty corePoolSize: Int,
  @JsonProperty maxConnTotal: Int,
  @JsonProperty maxConnectionsPerRoute: Int) extends Serializable

case class DatabaseConfig(
 @JsonProperty embeddedMode: Boolean,
 @JsonProperty driverClass: String,
 @JsonProperty user: String,
 @JsonProperty password: String,
 @JsonProperty url: String,
 @JsonProperty properties: Map[String, String],
 @JsonProperty maxWaitForConnection: Int,
 @JsonProperty validationQuery: String,
 @JsonProperty min: Int,
 @JsonProperty maxSize: Int,
 @JsonProperty checkConnectionWhileIdle: Boolean
)

/* Request / Response Formats */

case class ServiceResponse[T <: Product](
   val statusCode: Int,
   val errorMessage: Option[String] = None,
   val message: Option[T] = None
)
