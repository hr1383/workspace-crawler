package com.vicaya.database.dao.service

import com.vicaya.database.models.Username

object UsernameDao {
    def apply(baseDaoService: BaseDaoService): UsernameDao = {
        new UsernameDao(baseDaoService)
    }
}

class UsernameDao(baseDaoService: BaseDaoService) {

    val ctx = baseDaoService.session
    import ctx._

    val users = quote(querySchema[Username]("username"))

    def find(id: Long): Option[Username] = {
        run(query[Username].filter(u => u.id == lift(id))).headOption
    }

    def create(user: Username): Username = {
        val newId = run(query[Username].insert(lift(user)).returning(_.id))
        user.copy(id = newId)
    }

    def delete(id: Long): Boolean = {
        run(query[Username].filter(_.id == lift(id)).delete)
        true
    }

    def update(user: Username): Unit = {
        run(query[Username].filter(_.id == lift(user.id)).update(lift(user)))
    }
}