package kr.debop.catalina.session

import org.apache.catalina.Session
import org.apache.catalina.connector.{Request, Response}
import org.apache.catalina.valves.ValveBase
import org.slf4j.LoggerFactory

/**
 * RedisSessionHandlerValve
 * @author sunghyouk.bae@gmail.com
 */
class RedisSessionHandlerValve(var manager: RedisSessionManager) extends ValveBase {

  require(manager != null)

  private lazy val log = LoggerFactory.getLogger(getClass)

  override def invoke(request: Request, response: Response): Unit = {
    try {
      val next = getNext
      if (next != null)
        next.invoke(request, response)
    } catch {
      case e: Throwable =>
        log.error(s"Invoke 시 예외가 발생했습니다.", e)
    } finally {
      val session = request.getSessionInternal(false)
      storeOrRemoveSession(session)
      manager.afterRequest()
    }
  }

  private def storeOrRemoveSession(session: Session): Unit = {
    if (session == null)
      return

    try {
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

    } catch {
      case e: Throwable =>
        log.debug(s"Fail to save or remove session. session=$session", e)
    }
  }
}
