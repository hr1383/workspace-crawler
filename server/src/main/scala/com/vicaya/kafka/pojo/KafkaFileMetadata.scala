package com.vicaya.kafka.pojo

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, ObjectInputStream, ObjectOutputStream}

object KafkaSerializer {

  def serialise(fileMeta: KafkaFileMetadata): Array[Byte] = {
    val stream: ByteArrayOutputStream = new ByteArrayOutputStream()
    val oos = new ObjectOutputStream(stream)
    oos.writeObject(fileMeta)
    oos.close()
    stream.toByteArray
  }

  def deserialise[T](bytes: Array[Byte]): T = {
    val ois = new ObjectInputStream(new ByteArrayInputStream(bytes))
    val value = ois.readObject
    ois.close()
    value.asInstanceOf[T]
  }

}

case class KafkaFileMetadata(
  id: String,
  name: String,
  doc_type: String,
  size: Long,
  s3_location: String,
  uuid: String,
  source: String,
  last_modified: String)