package com.work.space.crawler.resources

import java.io.File

import com.datasift.dropwizard.scala.ScalaApplication
import com.datasift.dropwizard.scala.test.{ApplicationTest, BeforeAndAfterAllMulti}
import com.google.common.io.Resources
import com.work.space.crawler.configuration.{ServiceResponse, WorkSpaceCrawlerConfiguration}
import io.dropwizard.setup.Environment
import javax.ws.rs.client.{Client, Entity, WebTarget}
import javax.ws.rs.core.{MediaType, Response}
import org.scalatest.{FlatSpec, Matchers}

import scala.util.Try

object WorkSpaceCrawlerTestApp extends ScalaApplication[WorkSpaceCrawlerConfiguration] {

  override def run(conf: WorkSpaceCrawlerConfiguration, env: Environment): Unit = {
    val resource = new WorkSpaceResource()
    env.jersey().register(resource)
  }
}

//https://github.com/datasift/dropwizard-scala/blob/master/core/src/test/scala/com/datasift/dropwizard/scala/ScalaApplicationSpecIT.scala
class WorkSpaceResourceSpecIT extends FlatSpec with BeforeAndAfterAllMulti with Matchers {
  val testConfigName = "workspace-service-test.yaml"

  val app: ApplicationTest[WorkSpaceCrawlerConfiguration] = ApplicationTest(
    this,
    new File(Resources.getResource(testConfigName).toURI).getAbsolutePath
  ) {
    WorkSpaceCrawlerTestApp
  }

  lazy val client: Try[Client] = app.newClient("test")

  def request(): Try[WebTarget] = for {
    client <- client
    server <- app.server
  } yield { client.target(server.getURI) }

  def request(target: String): Try[WebTarget] = for {
    client <- client
    server <- app.server
  } yield { client.target(server.getURI.resolve(target)) }

  "POST /v1/cluster/status" should " get the cluster status from Spark Master" in {
    val result: Try[Response] = request("/v1/crawl/images/").map {
      _.request(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .post(Entity.json("http://www.zoomify.com/"))
    }

    val resp = result.map { r => r.readEntity[ServiceResponse](classOf[ServiceResponse]) }
    resp.get.message.isDefined shouldBe true
    resp.get.message.get shouldEqual "List()"
  }


}
