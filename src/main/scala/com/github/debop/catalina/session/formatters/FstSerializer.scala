package com.github.debop.catalina.session.formatters

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}

import com.github.debop.catalina.session._
import de.ruedigermoeller.serialization.FSTConfiguration
import org.slf4j.LoggerFactory

import scala.util.{Failure, Success, Try}

object FstSerializer {

  lazy val defaultCfg = FSTConfiguration.createDefaultConfiguration()

  def apply[T](): FstSerializer[T] = new FstSerializer[T]()
}

/**
 * Serialize/Deserialize by Fast-Serialization
 *
 * Created by debop on 2014. 3. 30.
 */
class FstSerializer[T] extends Serializer[T] {

  private lazy val log = LoggerFactory.getLogger(getClass)

  def defaultCfg: FSTConfiguration = FstSerializer.defaultCfg

  override def serialize(graph: T): Array[Byte] = {
    if (graph == null || graph == None)
      return Array.empty[Byte]

    using(new ByteArrayOutputStream()) { bos =>
      Try(defaultCfg.getObjectOutput(bos)) match {
        case Success(oos) =>
          oos.writeObject(graph, Seq[Class[_]](): _*)
          oos.flush()
          bos.toByteArray
        case Failure(e) =>
          log.error(s"Fail to serialize graph. $graph", e)
          Array.empty[Byte]
      }
    }
  }

  override def deserialize(bytes: Array[Byte]): T = {
    if (bytes == null || bytes.length == 0)
      return null.asInstanceOf[T]

    using(new ByteArrayInputStream(bytes)) { bis =>
      Try(defaultCfg.getObjectInput(bis)) match {
        case Success(ois) =>
          ois.readObject.asInstanceOf[T]

        case Failure(e) =>
          log.error(s"Fail to deserialize data.", e)
          null.asInstanceOf[T]
      }
    }
  }
}
