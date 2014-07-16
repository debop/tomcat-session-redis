import java.io.File

import kr.debop.catalina.session.{RedisSessionHandlerValve, RedisSessionManager}
import org.apache.catalina.Context
import org.apache.catalina.connector.Connector
import org.apache.catalina.core._
import org.apache.catalina.mbeans.GlobalResourcesLifecycleListener
import org.apache.catalina.startup.Tomcat
import org.slf4j.LoggerFactory

/**
 * TomcatLauncher
 * @author sunghyouk.bae@gmail.com
 */
object TomcatLauncher extends App {

  def port: Int = 8080
  def contextPath: String = "/"
  def protocol: String = "org.apache.coyote.http11.Http11Protocol"

  // TEST 이라서
  def resourceBase: String = "src/test/webapp"

  private lazy val log = LoggerFactory.getLogger(getClass)

  try {
    val tomcat = new Tomcat()

    tomcat.getServer.addLifecycleListener(new AprLifecycleListener)
    tomcat.getServer.addLifecycleListener(new JreMemoryLeakPreventionListener)
    tomcat.getServer.addLifecycleListener(new GlobalResourcesLifecycleListener)
    tomcat.getServer.addLifecycleListener(new ThreadLocalLeakPreventionListener)

    val connector: Connector = new Connector(protocol)
    connector.setPort(port)
    connector.setURIEncoding("utf-8")
    connector.setEnableLookups(false)

    tomcat.getService.addConnector(connector)
    tomcat.setConnector(connector)

    tomcat.setPort(port.toInt)

    val context: Context = tomcat.addWebapp(contextPath, new File(resourceBase).getAbsolutePath)

    initContext(context)

    tomcat.start()
    log.info(s"Start Tomcat Web Server")
    tomcat.getServer.await()

  } catch {
    case e: Throwable => log.error("TomcatLauncherSupport ERROR", e)
  }


  private final val SESSION_TIMEOUT: Int = 1

  private def initContext(ctx: Context) {

    ctx.setSessionTimeout(SESSION_TIMEOUT)

    ctx.addApplicationListener("kr.debop.catalina.session.example.web.listener.SessionListener")
    ctx.addApplicationListener("kr.debop.catalina.session.example.web.listener.SessionAttributeListener")

    ctx.setSessionCookieName("REDISSESSID")

    val sessionManager = new RedisSessionManager
    sessionManager.setDatabase(1)
    ctx.getPipeline.addValve(new RedisSessionHandlerValve(sessionManager))
    ctx.setManager(sessionManager)
  }
}
