package com.github.debop.catalina.session

import org.apache.catalina.Manager
import org.apache.catalina.session.StandardSession
import org.slf4j.LoggerFactory

/**
 * RedisSession
 * @author sunghyouk.bae@gmail.com
 */
class RedisSession(private[this] val manager: Manager) extends StandardSession(manager) {

  private lazy val log = LoggerFactory.getLogger(getClass)
}
