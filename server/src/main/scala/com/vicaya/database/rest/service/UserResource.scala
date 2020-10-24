package com.vicaya.database.rest.service

import java.time.Instant
import java.util.concurrent.atomic.AtomicLong

import com.vicaya.app.configuration.ServiceResponse
import com.vicaya.common.sidutil.Sid
import com.vicaya.database.dao.service.{BaseDaoService, UsernameDao}
import com.vicaya.database.models.Username
import javax.ws.rs.core.MediaType
import javax.ws.rs.{Consumes, GET, POST, Path, PathParam, Produces}
import org.slf4j.{Logger, LoggerFactory}

import scala.util.{Failure, Success, Try}

object UserResource {
    val logger: Logger = LoggerFactory.getLogger(classOf[UserResource])
    def apply(baseDaoService: BaseDaoService): UserResource = {
        new UserResource(baseDaoService)
    }
}

case class User(userName: String, accountSid: String)

@Path("/user")
class UserResource(baseDaoService: BaseDaoService) {

    import UserResource._
    val userDao: UsernameDao = new UsernameDao(baseDaoService)
    val atomicLong: AtomicLong = new AtomicLong()

    //curl -XGET http://localhost:9090/user/find/USe04103cdb8c528ea2b6f3a46a57b542d
    @GET
    @Path("/find/{sid}")
    @Consumes(Array(MediaType.APPLICATION_JSON))
    @Produces(Array(MediaType.APPLICATION_JSON))
    def findUser(@PathParam("sid") sid: String): ServiceResponse[Username] = {
        Try(userDao.find(sid)) match {
            case Success(value) =>
                successCase(value)
            case Failure(exception: Exception) =>
                failedCase(s"Failed to fetch user with $sid", exception)
        }
    }

    // e.g curl -XPOST -v http://localhost:9090/user/create -d '{"userName": "rohit","accountSid": "vicaya"}' -H "Accept: application/json" -H "Content-Type: application/json"
    @POST
    @Path("/create")
    @Consumes(Array(MediaType.APPLICATION_JSON))
    @Produces(Array(MediaType.APPLICATION_JSON))
    def create(username: User): ServiceResponse[Username] = {
        Try {
            userDao.create(Username(
                userSid = "",
                userName = username.userName,
                accountSid = username.accountSid,
                timeCreated = Instant.now().toEpochMilli,
                timeLastLogin = Instant.now().toEpochMilli,
                isActive = true
            ))
        } match {
            case Success(value) =>
                successCase(Some(value))
            case Failure(exception: Exception) =>
                failedCase(s"Failed to create user", exception)
        }
    }

    def successCase(value: Option[Username]): ServiceResponse[Username] = {
        ServiceResponse[Username](
            statusCode = 200,
            message = value
        )
    }

    def failedCase(msg: String, exception: Exception): ServiceResponse[Username] = {
        logger.error(s"Error $exception")
        ServiceResponse[Username](
            statusCode = 500,
            errorMessage = Some(msg)
        )
    }
}