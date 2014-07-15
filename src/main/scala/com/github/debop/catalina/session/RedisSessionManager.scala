package com.github.debop.catalina.session

import com.github.debop.catalina.session.formatters.FstSessionFormatter
import org.apache.catalina.session.ManagerBase
import org.apache.catalina.util.LifecycleSupport
import org.apache.catalina.{Lifecycle, LifecycleListener, Session}
import org.slf4j.LoggerFactory
import redis.RedisClient

/**
 * Tomcat Session 정보를 Redis 서버에서 관리하는 Manager입니다.
 *
 * @param redis [[RedisClient]] 인스턴스
 */
class RedisSessionManager(val redis: RedisClient) extends ManagerBase with Lifecycle {

  private lazy val log = LoggerFactory.getLogger(getClass)

  protected val NULL_SESSION = "null".getBytes

  protected val lifecycle = new LifecycleSupport(this)

  implicit val sessionFormatter = new FstSessionFormatter[Session]()

  override def getRejectedSessions: Int = 0

  override def load() {}

  override def unload() {}

  override def addLifecycleListener(listener: LifecycleListener) {
    lifecycle.addLifecycleListener(listener)
  }
  override def findLifecycleListeners(): Array[LifecycleListener] = {
    lifecycle.findLifecycleListeners()
  }
  override def removeLifecycleListener(listener: LifecycleListener) {
    lifecycle.removeLifecycleListener(listener)
  }

}
