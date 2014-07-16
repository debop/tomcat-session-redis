package kr.debop.catalina.session.example.web.listener

import javax.servlet.http.{HttpSessionAttributeListener, HttpSessionBindingEvent}

import org.slf4j.LoggerFactory

/**
 * SessionAttributeListener
 * @author sunghyouk.bae@gmail.com
 */
class SessionAttributeListener extends HttpSessionAttributeListener {

  private val log = LoggerFactory.getLogger(getClass)

  override def attributeAdded(se: HttpSessionBindingEvent) {
    log.info(s"Session attribute added. name=${se.getName}, value=${se.getValue}")
  }
  override def attributeReplaced(se: HttpSessionBindingEvent) {
    log.info(s"Session attribute replaced. name=${se.getName}, value=${se.getValue}")
  }
  override def attributeRemoved(se: HttpSessionBindingEvent) {
    log.info(s"Session attribute removed. name=${se.getName}, value=${se.getValue}")
  }
}
