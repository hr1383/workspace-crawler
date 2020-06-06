package com.work.space.crawler.services

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.slf4j.{Logger, LoggerFactory}

import scala.util.{Failure, Success, Try}
import collection.JavaConverters._

object BasicWebCrawlerService {
    def apply(): BasicWebCrawlerService = {
        new BasicWebCrawlerService()
    }
}

case class ImageMetaInfo(
 src: String,
 height: String,
 width: String,
 alt: String
)

class BasicWebCrawlerService {
    val logger: Logger = LoggerFactory.getLogger("com.work.space.crawler.service.BasicWebCrawlerService")

    def pageLink(link: String): Seq[ImageMetaInfo] = {
         Try(Jsoup.connect(link).get()) match {
            case Success(document) =>
                // Parse images on the page
                val images = document.select("img[src~=(?i)\\.(png|jpe?g|gif)]")
                fetchImageAttr(images.asScala.toSeq)
            case Failure(exception) =>
                logger.error(s"Failed to fetch information for $link", exception)
                Seq.empty[ImageMetaInfo]
        }
    }

    def fetchImageAttr(elements: Seq[Element]): Seq[ImageMetaInfo] = {
        elements.map(element => {
            ImageMetaInfo(
                element.attr("src"),
                element.attr("height"),
                element.attr("width"),
                element.attr("alt")
            )
        })
    }
}
