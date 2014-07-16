package kr.debop.catalina.session.event

import java.io.Serializable

abstract class RedisSessionEvent(val id: String) {
  override def toString: String = {
    getClass.getCanonicalName + s"# id=$id"
  }
}

case class RedisSessionCreatedEvent(override val id: String) extends RedisSessionEvent(id)

case class RedisSessionDestroyedEvent(override val id: String) extends RedisSessionEvent(id)


abstract class RedisSessionAttributeEvent(override val id: String,
                                          val name: String,
                                          val value: Serializable) extends RedisSessionEvent(id) {
  override def toString: String = {
    new StringBuilder()
    .append(getClass.getCanonicalName)
    .append("#")
    .append(s"id=$id")
    .append(s", name=$name")
    .append(s", value=$value")
    .toString()
  }
}

class RedisSessionAddAttributeEvent(override val id: String,
                                    override val name: String,
                                    override val value: Serializable)
  extends RedisSessionAttributeEvent(id, name, value) {}


case class RedisSessionRemoveAttributeEvent(override val id: String,
                                            override val name: String,
                                            override val value: Serializable)
  extends RedisSessionAttributeEvent(id, name, value) {}


case class RedisSessionReplaceAttributeEvent(override val id: String,
                                             override val name: String,
                                             override val value: Serializable)
  extends RedisSessionAttributeEvent(id, name, value) {}


