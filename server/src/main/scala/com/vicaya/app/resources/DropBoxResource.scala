package com.vicaya.app.resources

import java.util.concurrent.TimeUnit

import com.vicaya.app.configuration.ServiceResponse
import com.vicaya.connectors.{ConnectorService, DropBoxConnect}
import com.vicaya.database.models.Username
import com.vicaya.database.rest.service.UserResource.logger
import javax.ws.rs.container.{AsyncResponse, Suspended}
import javax.ws.rs.core.MediaType
import javax.ws.rs.{Consumes, GET, POST, Path, Produces, QueryParam}

object DropBoxResource {
  def apply(dropboxConnector: DropBoxConnect): DropBoxResource = new DropBoxResource(dropboxConnector)
}

@Path("/v1/dropbox")
class DropBoxResource(dropBoxConnect: DropBoxConnect) {

  //curl -XPOST http://localhost:8080/v1/dropbox/crawl -H 'Content-Type: application/json' -H "Accept: application/json"
  @POST
  @Path("/crawl")
  @Consumes(Array(MediaType.APPLICATION_JSON))
  @Produces(Array(MediaType.APPLICATION_JSON))
  def crawl(): Boolean = {
    dropBoxConnect.crawlThenPublish()
  }

  def successCase(value: Option[Username]): ServiceResponse[Username] = {
    ServiceResponse[Username](
      statusCode = 200,
      message = value
    )
  }

  def failedCase(msg: String, exception: Exception): ServiceResponse[Username] = {
    logger.error(s"Error $exception")
    ServiceResponse[Username](
      statusCode = 500,
      errorMessage = Some(msg)
    )
  }
}