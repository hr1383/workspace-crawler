package com.vicaya.web.crawler.resources

import com.datasift.dropwizard.scala.test.BeforeAndAfterAllMulti
import com.opentable.db.postgres.embedded.EmbeddedPostgres
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import io.getquill.{PostgresJdbcContext, SnakeCase}
import org.scalatest.{FlatSpec, Matchers}
import com.vicaya.database.dao.service.{BaseDaoService, UsernameDao}
import com.vicaya.database.models.Username
import java.time.Instant

class UsernameDaoTest extends FlatSpec with BeforeAndAfterAllMulti with Matchers {

    val server = EmbeddedPostgres.builder().setPort(5432).start()
    val ctx: PostgresJdbcContext[SnakeCase] = buildPostgres
    var connection: java.sql.Connection = _

    val baseDaoService: BaseDaoService = new BaseDaoService(ctx)
    val userDao: UsernameDao = new UsernameDao(baseDaoService)

    override def beforeAll(): Unit = {
        val statement = connection.createStatement()
        val createUsername = "" +
          "CREATE TABLE IF NOT EXISTS username (user_sid varchar NOT NULL, user_name varchar NOT NULL, account_sid varchar NOT NULL, time_created bigint, time_last_login bigint, is_active boolean NOT NULL);"
        statement.execute(createUsername)
    }

    override def afterAll(): Unit = {
        val statement = connection.createStatement()
        val deleteUsername = "DROP TABLE IF EXISTS username;"
        statement.execute(deleteUsername)
    }

    "PostgresQuery" should "used be able to create user " in {
        val username = Username(
            userSid = "",
            userName = "rohit",
            accountSid = "vicaya",
            timeCreated = Instant.now().toEpochMilli,
            timeLastLogin = Instant.now().toEpochMilli,
            isActive = true
        )
        val updatedUserName = userDao.create(username)
        println(s"New User Created with id:${updatedUserName}")
        userDao.find(updatedUserName.userSid).isDefined shouldBe true
        userDao.delete(updatedUserName.userSid)
    }

    "PostgresQuery" should "used be able to update user " in {
        val username = Username(
            userSid = "",
            userName = "rohit",
            accountSid = "vicaya",
            timeCreated = Instant.now().toEpochMilli,
            timeLastLogin = Instant.now().toEpochMilli,
            isActive = true
        )
        val updatedUserName = userDao.create(username)
        println(s"New User Created with id:${updatedUserName}")
        userDao.find(updatedUserName.userSid).isDefined shouldBe true
        val updatedUsername = Username(
            userSid = updatedUserName.userSid,
            userName = "rohit",
            accountSid = "vicaya",
            timeCreated = updatedUserName.timeCreated,
            timeLastLogin = Instant.now().toEpochMilli,
            isActive = false
        )
        userDao.update(updatedUsername)
        userDao.find(updatedUserName.userSid).isDefined shouldBe true
        userDao.find(updatedUserName.userSid).get.isActive shouldBe false
        userDao.delete(updatedUserName.userSid)
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