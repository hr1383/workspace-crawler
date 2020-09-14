package com.vicaya.app.resources


import com.vicaya.connectors.BoxConnect
import javax.ws.rs.core.MediaType
import javax.ws.rs.{Consumes, GET, POST, Path, Produces, QueryParam}

object BoxResource {
  def apply(boxConnector: BoxConnect): BoxResource = new BoxResource(boxConnector)
}

@Path("/v1/box")
class BoxResource(boxConnect: BoxConnect) {

  @POST
  @Path("/crawl")
  @Consumes(Array(MediaType.APPLICATION_JSON))
  @Produces(Array(MediaType.APPLICATION_JSON))
  def crawl(): Boolean = {
    boxConnect.crawlThenPublish()
  }
}
