package com.work.space.crawler.resources

import com.datasift.dropwizard.scala.test.BeforeAndAfterAllMulti
import org.scalatest.{FlatSpec, Matchers}
import com.opentable.db.postgres.embedded.EmbeddedPostgres
import io.getquill._
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import org.flywaydb.core.Flyway

case class City(
    id: Int,
    name: String,
    countryCode: String,
    district: String,
    population: Int
)

case class Country(
    code: String,
    name: String,
    continent: String,
    region: String,
    surfaceArea: Double,
    indepYear: Option[Int],
    population: Int,
    lifeExpectancy: Option[Double],
    gnp: Option[scala.math.BigDecimal],
    gnpold: Option[scala.math.BigDecimal],
    localName: String,
    governmentForm: String,
    headOfState: Option[String],
    capital: Option[Int],
    code2: String
)

case class CountryLanguage(
    countrycode: String,
    language: String,
    isOfficial: Boolean,
    percentage: Double
)

class WorkSpaceDoaTest extends FlatSpec with BeforeAndAfterAllMulti with Matchers {

    val server = EmbeddedPostgres.builder().setPort(5435).start()
    val pgDataSource = new org.postgresql.ds.PGSimpleDataSource()
    pgDataSource.setUser("postgres")
    val config = new HikariConfig()
    config.setDataSource(pgDataSource)
    val ds = new HikariDataSource(config)
    val ctx = new PostgresJdbcContext(LowerCase, ds)

    override def beforeAll(): Unit = {
        val flyway = Flyway.configure.dataSource(pgDataSource).locations("db").load()
        flyway.migrate()
    }

    "PostgresQuery" should "be able to run select query " in {
        import ctx._
        implicit val CountrySchemaMeta = schemaMeta[Country]("Country")
        val response = ctx.run(query[Country].map(c => (c.name, c.continent)))
        response.map(println)
    }
}

