package com.vicaya.app.resources


import com.vicaya.connectors.BoxConnect
import javax.ws.rs.core.MediaType
import javax.ws.rs.{Consumes, GET, POST, Path, Produces, QueryParam}

object BoxResource {
  def apply(boxConnector: BoxConnect): BoxResource = new BoxResource(boxConnector)
}

@Path("/v1/box")
class BoxResource(boxConnect: BoxConnect) {

  //curl -XPOST http://localhost:8080/v1/box/crawl -H 'Content-Type: application/json' -H "Accept: application/json"
  @POST
  @Path("/crawl")
  @Consumes(Array(MediaType.APPLICATION_JSON))
  @Produces(Array(MediaType.APPLICATION_JSON))
  def crawl(): Boolean = {
    boxConnect.crawlThenPublish()
  }

  //curl -XPOST http://localhost:8080/v1/box/download -H 'Content-Type: application/json' -H "Accept: application/json"
  @POST
  @Path("/download")
  @Consumes(Array(MediaType.APPLICATION_JSON))
  @Produces(Array(MediaType.APPLICATION_JSON))
  def download(): Boolean = {
    boxConnect.crawlThenDownload()
  }
}
