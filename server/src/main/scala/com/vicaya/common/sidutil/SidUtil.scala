package com.vicaya.common.sidutil

import java.net.InetAddress
import java.security.SecureRandom
import java.time.Instant

import org.apache.commons.codec.digest.DigestUtils

object SidUtil {

  // constants
  final val RANDOM_ALG: String = "SHA1PRNG"
  final val RANDOM_PROVIDER: String = "SUN"
  final val BYTE_LIMIT: Int = 64
  val bytes = new Array[Byte](BYTE_LIMIT)

  final val srand: SecureRandom = SecureRandom.getInstance(RANDOM_ALG, RANDOM_PROVIDER)
  final val ADDR: String = InetAddress.getLocalHost.toString

  def generateGuidHash: String = {
    srand.nextBytes(bytes)
    val random: String = new String(bytes)
    DigestUtils.md5Hex(s"$ADDR $random ${Instant.now()}")
  }

  def generateGuidHash(prefix: String): String = {
    prefix + generateGuidHash
  }
}
