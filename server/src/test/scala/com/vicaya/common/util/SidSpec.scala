package com.vicaya.common.util

import com.datasift.dropwizard.scala.test.BeforeAndAfterAllMulti
import org.scalatest.{FlatSpec, Matchers}
import com.vicaya.common.util.prefix._

class SidSpec extends FlatSpec with BeforeAndAfterAllMulti with Matchers {

  "SidUtil" should "be able to generate account sid" in {
    val accountSid: String = SidUtil.generateGuidHash(AccountSid)
    println(s"AccountSid: [$accountSid]")
    accountSid.contains(AccountSid) shouldBe true
  }

  "SidUtil" should "be able to generate user sid" in {
    val userSid: String = SidUtil.generateGuidHash(UserSid)
    println(s"UserSid: [$userSid]")
    userSid.contains(UserSid) shouldBe true
  }

  "SidUtil" should "be able to generate role sid" in {
    val roleSid: String = SidUtil.generateGuidHash(RoleSid)
    println(s"RoleSid: [$roleSid]")
    roleSid.contains(RoleSid) shouldBe true
  }

  "SidUtil" should "be able to generate connector sid" in {
    val connectorSid: String = SidUtil.generateGuidHash(ConnectorSid)
    println(s"RoleSid: [$connectorSid]")
    connectorSid.contains(ConnectorSid) shouldBe true
  }
}
