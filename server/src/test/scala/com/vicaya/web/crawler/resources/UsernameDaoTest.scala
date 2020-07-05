package com.vicaya.web.crawler.resources

import com.datasift.dropwizard.scala.test.BeforeAndAfterAllMulti
import com.opentable.db.postgres.embedded.EmbeddedPostgres
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import io.getquill.{PostgresJdbcContext, SnakeCase}
import org.scalatest.{FlatSpec, Matchers}
import com.vicaya.database.dao.service.{BaseDaoService, UsernameDao}
import com.vicaya.database.models.Username

class UsernameDaoTest extends FlatSpec with BeforeAndAfterAllMulti with Matchers {

    val server = EmbeddedPostgres.builder().setPort(5432).start()
    val ctx: PostgresJdbcContext[SnakeCase] = buildPostgres
    var connection: java.sql.Connection = _

    val baseDaoService: BaseDaoService = new BaseDaoService(ctx)
    val userDao: UsernameDao = new UsernameDao(baseDaoService)

    override def beforeAll(): Unit = {
        val statement = connection.createStatement()
        val createUsername = "CREATE TABLE IF NOT EXISTS username (id SERIAL NOT NULL,name varchar NOT NULL,company varchar NOT NULL,is_active boolean NOT NULL);"
        statement.execute(createUsername)
    }

    override def afterAll(): Unit = {
        val statement = connection.createStatement()
        val deleteUsername = "DROP TABLE IF EXISTS username;"
        statement.execute(deleteUsername)
    }

    "PostgresQuery" should "used be able to create user " in {
        val username = Username(
            name = "rohit",
            company = "vicaya",
            isActive = true
        )
        val updatedUserName = userDao.create(username)
        println(s"New User Created with id:${updatedUserName}")
        userDao.find(updatedUserName.id).isDefined shouldBe true
        userDao.delete(updatedUserName.id)
    }

    "PostgresQuery" should "used be able to update user " in {
        val username = Username(
            name = "rohit",
            company = "vicaya",
            isActive = true,
            id = 1
        )
        val updatedUserName = userDao.create(username)
        println(s"New User Created with id:${updatedUserName}")
        userDao.find(updatedUserName.id).isDefined shouldBe true
        val updatedUsername = Username(
            name = "rohit",
            company = "vicaya",
            isActive = false,
            id = 1
        )
        userDao.update(updatedUsername)
        userDao.find(updatedUserName.id).isDefined shouldBe true
        userDao.find(updatedUserName.id).get.isActive shouldBe false
        userDao.delete(updatedUserName.id)
    }


    def buildPostgres: PostgresJdbcContext[SnakeCase] = {
        val pgDataSource = new org.postgresql.ds.PGSimpleDataSource()
        pgDataSource.setUser("postgres")
        val config = new HikariConfig()
        config.setDataSource(pgDataSource)
        val ds = new HikariDataSource(config)
        val ctx = new PostgresJdbcContext[SnakeCase](SnakeCase, ds)
        connection = ds.getConnection
        ctx
    }
}