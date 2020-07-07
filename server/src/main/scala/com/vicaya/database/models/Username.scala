package com.vicaya.database.models

import com.vicaya.common.util.Sid
import com.vicaya.common.util.prefix._

case class Username(
 name: String,
 company: String,
 isActive: Boolean,
 id: Long = 0
) extends Sid(UserSid)