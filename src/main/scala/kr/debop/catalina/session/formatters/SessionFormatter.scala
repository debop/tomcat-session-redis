package kr.debop.catalina.session.formatters

import akka.util.ByteString
import kr.debop.catalina.session.serializer._
import redis.ByteStringFormatter

abstract class AbstractSessionFormatter[T](val serializer: Serializer[T]) extends ByteStringFormatter[T] {

  override def serialize(data: T): ByteString = {
    data match {
      case null => ByteString.empty
      case _ => ByteString(serializer.serialize(data))
    }
  }
  override def deserialize(bs: ByteString): T = {
    bs match {
      case null => null.asInstanceOf[T]
      case _ => serializer.deserialize(bs.toArray)
    }
  }
}

class BinarySessionFormatter[T] extends AbstractSessionFormatter[T](BinarySerializer[T]()) {}

class FstSessionFormatter[T] extends AbstractSessionFormatter[T](FstSerializer[T]()) {}

class SnappyFstSessionFormatter[T]
  extends AbstractSessionFormatter[T](SnappySerializer[T](FstSerializer[T]())) {}
