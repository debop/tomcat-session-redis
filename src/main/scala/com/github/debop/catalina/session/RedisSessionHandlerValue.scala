package com.github.debop.catalina.session

import org.apache.catalina.Session
import org.apache.catalina.connector.{Request, Response}
import org.apache.catalina.valves.ValveBase
import org.slf4j.LoggerFactory

/**
 * RedisSessionHandlerValue
 * @author sunghyouk.bae@gmail.com
 */
class RedisSessionHandlerValue(private[this] val manager: RedisSessionManager) extends ValveBase {

  require(manager != null)

  private lazy val log = LoggerFactory.getLogger(getClass)

  override def invoke(request: Request, response: Response): Unit = {
    try {
      getNext.invoke(request, response)
    } finally {
      val session = request.getSessionInternal(false)
      storeOrRemoveSession(session)
      manager.afterRequest()
    }
  }

  private def storeOrRemoveSession(session: Session) {
    try {
      if (session != null) {
        if (session.isValid) {
          log.trace(s"Request with session completed. saving session ${session.getId}")
          if (session.getSession != null) {
            log.trace(s"HTTP Session present, saving ${session.getId}")
            manager.save(session)
          } else {
            log.trace(s"No HTTP Session present, No saving ${session.getId}")
          }
        } else {
          log.trace(s"HTTP Session has been invalided, removing ${session.getId}")
          manager.remove(session)
        }
      }
    } catch {
      case e: Throwable =>
        log.debug(s"Fail to save or remove session. session=$session", e)
    }
  }
}
