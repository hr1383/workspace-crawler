package com.vicaya.app.resources

import com.vicaya.app.response.Document
import javax.ws.rs.{GET, Path, Produces}
import com.vicaya.connectors.{ConnectorService}
import javax.ws.rs.core.MediaType

object ServiceResource {

  def apply(connectorService: ConnectorService): ServiceResource = new ServiceResource(connectorService)
}

@Path("/v1/search")
class ServiceResource(connectorService: ConnectorService) {

  @GET
  @Produces(Array(MediaType.APPLICATION_JSON))
  def search(text: String): Seq[Document] = {
    connectorService.search(text)
  }
}
