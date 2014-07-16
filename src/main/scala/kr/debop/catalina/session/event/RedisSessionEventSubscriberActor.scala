package kr.debop.catalina.session.event

import java.net.InetSocketAddress
import javax.servlet.http.{HttpSessionAttributeListener, HttpSessionBindingEvent, HttpSessionEvent, HttpSessionListener}

import akka.actor.Props
import com.google.gson.Gson
import kr.debop.catalina.session.{RedisSession, RedisSessionKeys, RedisSessionManager}
import org.apache.catalina.Context
import org.apache.catalina.core.StandardContext
import org.apache.tomcat.util.ExceptionUtils
import org.apache.tomcat.util.res.StringManager
import redis.actors.RedisSubscriberActor
import redis.api.pubsub.{Message, PMessage}

import scala.util.Try

object RedisSessionEventSubscriberActor {
  implicit val actorSystem = akka.actor.ActorSystem()

  def apply(manager: RedisSessionManager) = {
    val address = new InetSocketAddress(manager.host, manager.port)
    actorSystem.actorOf(Props(classOf[RedisSessionEventSubscriberActor],
                               manager,
                               address,
                               Seq(RedisSessionKeys.SESSION_CHANNEL),
                               Nil))
  }
}

/**
 * Session 생성/삭제/속성 변화를 리스닝합니다.
 * @author sunghyouk.bae@gmail.com
 */
class RedisSessionEventSubscriberActor(val manager: RedisSessionManager,
                                       override val address: InetSocketAddress,
                                       val channels: Seq[String] = Seq(RedisSessionKeys.SESSION_CHANNEL),
                                       val patterns: Seq[String] = Nil)
  extends RedisSubscriberActor(address, channels, patterns) {

  private val sm = StringManager.getManager(org.apache.catalina.session.Constants.Package)
  private lazy val gson: Gson = new Gson()

  override def onMessage(m: Message) {
    log.debug(s"Event from ${m.channel}: ${m.data}")

    try {
      val event = gson.fromJson(m.data, classOf[RedisSessionEvent])

      event match {
        case evt: RedisSessionCreatedEvent => sessionCreatedEvent(evt)
        case evt: RedisSessionDestroyedEvent => sessionDestroyedEvent(evt)
        case evt: RedisSessionAddAttributeEvent => sessionAttributeAddEvent(evt)
        case evt: RedisSessionRemoveAttributeEvent => sessionAttributeRemoveEvent(evt)
        case evt: RedisSessionReplaceAttributeEvent => sessionAttributeReplaceEvent(evt)
      }

    } catch {
      case e: Throwable =>
        log.warning(s"Can't deserialize event. message=${m.data}", e)
    }
  }

  override def onPMessage(pm: PMessage) {
    log.debug(s"PMessage ${pm.patternMatched} from ${pm.channel}: ${pm.data}")
  }

  private def fireContainerEvent(context: Context, typeStr: String, data: AnyRef) {
    if (context != null && context.isInstanceOf[StandardContext]) {
      context.fireContainerEvent(typeStr, data)
    }
  }

  private def sessionCreatedEvent(sessionCreatedEvent: RedisSessionCreatedEvent) {
    val context = manager.getContext
    val listeners: Array[AnyRef] = context.getApplicationLifecycleListeners

    if (listeners != null) {
      val session = new RedisSession(manager)
      session.setId(sessionCreatedEvent.id)
      val event = new HttpSessionEvent(session)

      listeners.toSeq
      .foreach {
        case listener: HttpSessionListener =>
          try {
            fireContainerEvent(context, "beforeSessionCreated", listener)
            listener.sessionCreated(event)
            fireContainerEvent(context, "afterSessionCreated", listener)
          } catch {
            case e: Throwable =>
              ExceptionUtils.handleThrowable(e)
              Try {fireContainerEvent(context, "afterSessionCreated", listener)}
              manager.getContext.getLogger.error(sm.getString("standardSession.sessionEvent"), e)
          }
      }
    }
  }

  private def sessionDestroyedEvent(sessionDestroyedEvent: RedisSessionDestroyedEvent) {
    val context = manager.getContext
    val listeners: Array[AnyRef] = context.getApplicationLifecycleListeners

    if (listeners != null) {
      val session = new RedisSession(manager)
      session.setId(sessionDestroyedEvent.id)
      val event = new HttpSessionEvent(session)

      listeners.toSeq
      .foreach {
        case listener: HttpSessionListener =>
          try {
            fireContainerEvent(context, "beforeSessionDestroyed", listener)
            listener.sessionDestroyed(event)
            fireContainerEvent(context, "afterSessionDestroyed", listener)
          } catch {
            case e: Throwable =>
              ExceptionUtils.handleThrowable(e)
              Try {fireContainerEvent(context, "afterSessionDestroyed", listener)}
              manager.getContext.getLogger.error(sm.getString("standardSession.sessionEvent"), e)
          }
      }
    }
  }

  private def sessionAttributeAddEvent(sessionAddAttributeEvent: RedisSessionAddAttributeEvent) {
    val context = manager.getContext
    val listeners: Array[AnyRef] = context.getApplicationLifecycleListeners

    if (listeners != null) {
      val session = new RedisSession(manager)
      session.setId(sessionAddAttributeEvent.id)

      listeners.toSeq
      .foreach {
        case listener: HttpSessionAttributeListener =>
          try {
            fireContainerEvent(context, "beforeSessionAttributeAdded", listener)
            val event = new HttpSessionBindingEvent(session, sessionAddAttributeEvent.name, sessionAddAttributeEvent.value)
            listener.attributeAdded(event)
            fireContainerEvent(context, "afterSessionAttributeAdded", listener)
          } catch {
            case e: Throwable =>
              ExceptionUtils.handleThrowable(e)
              Try {fireContainerEvent(context, "afterSessionAttributeAdded", listener)}
              manager.getContext.getLogger.error(sm.getString("standardSession.attributeEvent"), e)
          }
      }
    }
  }

  private def sessionAttributeRemoveEvent(sessionRemoveAttributeEvent: RedisSessionRemoveAttributeEvent) {
    val context = manager.getContext
    val listeners: Array[AnyRef] = context.getApplicationLifecycleListeners

    if (listeners != null) {
      val session = new RedisSession(manager)
      session.setId(sessionRemoveAttributeEvent.id)

      listeners.toSeq
      .foreach {
        case listener: HttpSessionAttributeListener =>
          try {
            fireContainerEvent(context, "beforeSessionAttributeRemoved", listener)
            val event = new HttpSessionBindingEvent(session, sessionRemoveAttributeEvent.name, sessionRemoveAttributeEvent.value)
            listener.attributeRemoved(event)
            fireContainerEvent(context, "afterSessionAttributeRemoved", listener)
          } catch {
            case e: Throwable =>
              ExceptionUtils.handleThrowable(e)
              Try {fireContainerEvent(context, "afterSessionAttributeRemoved", listener)}
              manager.getContext.getLogger.error(sm.getString("standardSession.attributeEvent"), e)
          }
      }
    }
  }

  private def sessionAttributeReplaceEvent(sessionReplaceAttributeEvent: RedisSessionReplaceAttributeEvent) {
    val context = manager.getContext
    val listeners: Array[AnyRef] = context.getApplicationLifecycleListeners

    if (listeners != null) {
      val session = new RedisSession(manager)
      session.setId(sessionReplaceAttributeEvent.id)

      listeners.toSeq
      .foreach {
        case listener: HttpSessionAttributeListener =>
          try {
            fireContainerEvent(context, "beforeSessionAttributeReplaced", listener)
            val event = new HttpSessionBindingEvent(session, sessionReplaceAttributeEvent.name, sessionReplaceAttributeEvent.value)
            listener.attributeReplaced(event)
            fireContainerEvent(context, "afterSessionAttributeReplaced", listener)
          } catch {
            case e: Throwable =>
              ExceptionUtils.handleThrowable(e)
              Try {fireContainerEvent(context, "afterSessionAttributeReplaced", listener)}
              manager.getContext.getLogger.error(sm.getString("standardSession.attributeEvent"), e)
          }
      }
    }
  }
}

