package com.vicaya.database.models

case class Username(
 name: String,
 company: String,
 isActive: Boolean,
 id: Long = 0
)