package kr.debop.catalina.session.example.web.listener

import javax.servlet.http.{HttpSessionEvent, HttpSessionListener}

import org.slf4j.LoggerFactory

/**
 * SessionListener
 * @author sunghyouk.bae@gmail.com
 */
class SessionListener extends HttpSessionListener {

  private val log = LoggerFactory.getLogger(getClass)

  override def sessionCreated(se: HttpSessionEvent) {
    log.info(s"Session created: ${se.getSession.getId}")
  }
  override def sessionDestroyed(se: HttpSessionEvent) {
    log.info(s"Session destroyed: ${se.getSession.getId}")
  }
}
