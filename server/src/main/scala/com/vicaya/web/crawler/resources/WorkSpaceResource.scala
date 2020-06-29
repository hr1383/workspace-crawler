package com.vicaya.web.crawler.resources

import com.vicaya.web.crawler.services.BasicWebCrawlerService
import javax.ws.rs._
import org.slf4j.{Logger, LoggerFactory}

object WorkSpaceResource {
  val logger: Logger = LoggerFactory.getLogger(classOf[WorkSpaceResource])
}

@Path("/v1")
class WorkSpaceResource() {

  val basicWebCrawlerService: BasicWebCrawlerService = new BasicWebCrawlerService()

//  @POST
//  @Path("/crawl/images")
//  @Consumes(Array(MediaType.APPLICATION_JSON))
//  @Produces(Array(MediaType.APPLICATION_JSON))
//  def crawlImages(url: String): ServiceResponse[String] = {
//
//    val imageMetaInfo = basicWebCrawlerService.pageLink(url)
//    val allImages = imageMetaInfo.map(_.src).mkString(",")
//    ServiceResponse[String](
//      statusCode = 200,
//      message = Some(allImages)
//    )
//  }

}