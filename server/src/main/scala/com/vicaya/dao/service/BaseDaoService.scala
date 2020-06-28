package com.vicaya.dao.service

import io.getquill.{PostgresJdbcContext, SnakeCase}


object BaseDaoService {

    def apply(ctx: PostgresJdbcContext[SnakeCase]): BaseDaoService = new BaseDaoService(ctx)
}

class BaseDaoService(ctx: PostgresJdbcContext[SnakeCase]) {

    val session: PostgresJdbcContext[SnakeCase] = ctx
}