package com.vicaya.app.resources

import java.util.concurrent.TimeUnit

import com.vicaya.app.response.Document
import javax.ws.rs.{GET, Path, Produces, QueryParam}
import com.vicaya.connectors.ConnectorService
import javax.ws.rs.container.{AsyncResponse, Suspended}
import javax.ws.rs.core.MediaType

object ServiceResource {

  def apply(connectorService: ConnectorService): ServiceResource = new ServiceResource(connectorService)
}

@Path("/v1/search")
class ServiceResource(connectorService: ConnectorService) {

  @GET
  @Produces(Array(MediaType.APPLICATION_JSON))
  def search(@QueryParam("text") text: String,
             @Suspended asyncResponse: AsyncResponse): Unit = {
    asyncResponse.setTimeout(30, TimeUnit.SECONDS)
    asyncResponse.resume(connectorService.search(text))
  }
}
