package com.github.debop.catalina.session.formatters

/**
 * Serializer
 * @author sunghyouk.bae@gmail.com
 */
trait Serializer[T] {

  def serialize(graph: T): Array[Byte]

  def deserialize(bytes: Array[Byte]): T
}
