package kr.debop.catalina.session.serializer

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, ObjectInputStream, ObjectOutputStream}

import kr.debop.catalina.session._

object BinarySerializer {
  def apply[T](): BinarySerializer[T] = new BinarySerializer[T]()
}
/**
 * BinarySerializer
 * @author sunghyouk.bae@gmail.com
 */
class BinarySerializer[T] extends Serializer[T] {

  /**
   * 객체를 직렬화 합니다.
   * @param graph 직렬화할 객체
   * @return 직렬화된 정보를 가진 바이트 배열
   */
  @inline
  def serialize(graph: T): Array[Byte] = {
    if (graph == null)
      return Array.emptyByteArray

    val bos = new ByteArrayOutputStream()

    using(new ObjectOutputStream(bos)) { oos =>
      oos.writeObject(graph)
      oos.flush()
      bos.toByteArray
    }
  }

  /**
   * 직렬화된 바이트 배열을 역직렬화하여 객체로 변환합니다.
   * @param bytes 직렬화된 바이트 배열
   * @return 역직렬화된 객체 정보
   */
  @inline
  def deserialize(bytes: Array[Byte]): T = {
    if (bytes == null || bytes.length == 0)
      return null.asInstanceOf[T]

    using(new ByteArrayInputStream(bytes)) { bis =>
      val ois = new ObjectInputStream(bis)
      using(ois) { input =>
        input.readObject().asInstanceOf[T]
      }
    }
  }
}
