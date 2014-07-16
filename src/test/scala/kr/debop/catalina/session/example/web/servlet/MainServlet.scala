package kr.debop.catalina.session.example.web.servlet

import java.util.Date

import kr.debop.catalina.session.example.web.model.TestObject
import org.scalatra.ScalatraServlet
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._

/**
 * MainServlet
 * @author sunghyouk.bae@gmail.com
 */
class MainServlet extends ScalatraServlet {

  private lazy val log = LoggerFactory.getLogger(getClass)

  var i: Int = 0

  get("/") {

    log.info("--------- begin servlet --------------")

    contentType = "text/plain"

    if (session.isNew) {
      session.setAttribute("new", "true")

      response.writer.println("new session")
      response.writer.println(s"session-id=${session.getId}")
      response.writer.println(s"session-class=${session.getClass.getCanonicalName}")
    } else {
      session.removeAttribute("new")

      log.debug(s"session i=$i")
      session.setAttribute("i", i)
      session.setAttribute("obj", TestObject(System.currentTimeMillis()))

      i += 1

      response.writer.println("old session")
      response.writer.println(s"session id=${session.getId}")
      response.writer.println(s"session class=${session.getClass.getCanonicalName}")
      response.writer.println(s"creation-time=${new Date(session.getCreationTime)}")
      response.writer.println(s"last-access-time=${new Date(session.getLastAccessedTime)}")

      val attributes = session.getAttributeNames.asScala.toList
      response.writer.println(s"attributes:${attributes.mkString}")

      val obj = session.getAttribute("obj")
      response.writer.println(s"obj=$obj")
    }

    request.getHeaderNames.asScala.foreach { headerName =>
      request.getHeaders(headerName).asScala.foreach { headerValue =>
        response.getWriter.println(s"Header=$headerName, Value=$headerValue")
      }
    }

    log.info("---------- end servlet -----------")

  }

}
