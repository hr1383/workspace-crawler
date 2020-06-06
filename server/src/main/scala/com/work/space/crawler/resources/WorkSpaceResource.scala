package com.work.space.crawler.resources

import com.work.space.crawler.configuration.ServiceResponse
import javax.ws.rs._
import javax.ws.rs.core.MediaType
import org.slf4j.{Logger, LoggerFactory}

object WorkSpaceResource {
  val logger: Logger = LoggerFactory.getLogger(classOf[WorkSpaceResource])
}

@Path("/v1")
class WorkSpaceResource() {

  @POST
  @Path("/submit/app/{appId}")
  @Consumes(Array(MediaType.APPLICATION_JSON))
  @Produces(Array(MediaType.APPLICATION_JSON))
  def submitJob(@PathParam("appId") appId: String): ServiceResponse = {

    ServiceResponse(
      statusCode = 200,
      message = Some(
        s"Success. appId=$appId"
      )
    )
  }

  @GET
  @Path("/cluster/status")
  @Consumes(Array(MediaType.APPLICATION_JSON))
  @Produces(Array(MediaType.APPLICATION_JSON))
  def clusterStatus(): ServiceResponse = {
    ServiceResponse(
      statusCode = 200,
      message = Some("OK")
    )
  }

}