import javax.servlet.ServletContext

import kr.debop.catalina.session.example.web.servlet.MainServlet
import org.scalatra.LifeCycle

/**
 * ScalatraBootstrap
 * @author sunghyouk.bae@gmail.com
 */
class ScalatraBootstrap extends LifeCycle {

  override def init(context: ServletContext) {
    context.mount(new MainServlet(), "/*")
  }
}
