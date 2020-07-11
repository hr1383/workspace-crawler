package com.vicaya.common.sidutil

object Sid {
  def apply(prefix: String): Sid = {
    new Sid(prefix)
  }
}

class Sid(prefix: String) extends Serializable {
    var sid: String = _
    def Sid(): Unit = {
      sid = SidUtil.generateGuidHash(prefix)
    }
}
