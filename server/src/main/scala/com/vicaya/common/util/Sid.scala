package com.vicaya.common.util

abstract class Sid(prefix: String) extends Serializable {
    var sid: String = _
    def Sid(): Unit = {
      sid = SidUtil.generateGuidHash(prefix)
    }
}
