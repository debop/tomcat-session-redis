package kr.debop.catalina.session.event

import java.io.Serializable

case class RedisSessionEvent(id: String)

case class RedisSessionAddAttributeEvent(id: String, name: String, value: Serializable)

case class RedisSessionAttributeEvent(id: String, name: String, value: Serializable)

case class RedisSessionDestroyedEvent(id: String)

case class RedisSessionCreatedEvent(id: String)

case class RedisSessionRemoveAttributeEvent(id: String, name: String, value: Serializable)

case class RedisSessionReplaceAttributeEvent(id: String, name: String, value: Serializable)

