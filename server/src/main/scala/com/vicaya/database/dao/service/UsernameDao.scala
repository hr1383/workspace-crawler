package com.vicaya.database.dao.service

import com.vicaya.database.models.Username
import com.vicaya.common.util.prefix.UserSid
import com.vicaya.common.util.{Sid, SidUtil}
import org.slf4j.{Logger, LoggerFactory}

object UsernameDao {
    val logger: Logger = LoggerFactory.getLogger(classOf[UsernameDao])
    def apply(baseDaoService: BaseDaoService): UsernameDao = {
        new UsernameDao(baseDaoService)
    }
}

class UsernameDao(baseDaoService: BaseDaoService) {

    val ctx = baseDaoService.session
    import ctx._
    import UsernameDao._

    val users = quote(querySchema[Username]("username"))

    def find(id: String): Option[Username] = {
        run(query[Username].filter(u => u.userSid == lift(id))).headOption
    }

    def create(user: Username): Username = {
        val userWithSid = user.copy(userSid = SidUtil.generateGuidHash(UserSid))
        val updatedUserSid = run(query[Username].insert(lift(userWithSid)).returning(_.userSid))
        println(s"Going to create user: $updatedUserSid")
        val userCreated = userWithSid.copy(userSid = updatedUserSid)
        println(s"User is Created: $userCreated")
        userCreated
    }

    def delete(id: String): Boolean = {
        run(query[Username].filter(_.userSid == lift(id)).delete)
        true
    }

    def update(user: Username): Unit = {
        run(query[Username].filter(_.userSid == lift(user.userSid)).update(lift(user)))
    }
}