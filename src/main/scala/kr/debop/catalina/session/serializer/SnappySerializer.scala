package kr.debop.catalina.session.serializer

import org.xerial.snappy.Snappy

object SnappySerializer {

  def apply[T](inner: Serializer[T] = BinarySerializer[T]()): SnappySerializer[T] = {
    new SnappySerializer[T](inner)
  }
}

class SnappySerializer[T](val inner: Serializer[T]) extends Serializer[T] {

  override def serialize(graph: T): Array[Byte] = {
    if (graph == null || graph == None)
      return Array[Byte]()

    Snappy.compress(inner.serialize(graph))
  }

  override def deserialize(bytes: Array[Byte]): T = {
    if (bytes == null || bytes.length == 0)
      return null.asInstanceOf[T]

    inner.deserialize(Snappy.uncompress(bytes))
  }
}
