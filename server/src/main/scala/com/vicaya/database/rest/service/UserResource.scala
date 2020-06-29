package com.vicaya.database.rest.service

import java.util.concurrent.atomic.AtomicLong

import com.vicaya.app.configuration.ServiceResponse
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

case class User(name: String, company: String)

@Path("/user")
class UserResource(baseDaoService: BaseDaoService) {

    import UserResource._
    val userDao: UsernameDao = new UsernameDao(baseDaoService)
    val atomicLong: AtomicLong = new AtomicLong()

    //curl -XGET http://localhost:9460/user/find/1
    @GET
    @Path("/find/{id}")
    @Consumes(Array(MediaType.APPLICATION_JSON))
    @Produces(Array(MediaType.APPLICATION_JSON))
    def findUser(@PathParam("id") id: Long): ServiceResponse[Username] = {
        Try(userDao.find(id)) match {
            case Success(value) =>
                successCase(value)
            case Failure(exception: Exception) =>
                failedCase(s"Failed to fetch user with $id", exception)
        }
    }

    // e.g curl - XPOST - v http://localhost:9460/user/create -d '{"name": "rohit","company": "vicaya"}' -H "Accept: application/json" -H "Content-Type: application/json"
    @POST
    @Path("/create")
    @Consumes(Array(MediaType.APPLICATION_JSON))
    @Produces(Array(MediaType.APPLICATION_JSON))
    def create(username: User): ServiceResponse[Username] = {
        Try {
            userDao.create(Username(
                name = username.name,
                company = username.company,
                isActive = true,
                id = atomicLong.incrementAndGet()
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