package kr.debop.catalina.session

import kr.debop.catalina.session.event.RedisSessionEventSubscriberActor
import org.apache.catalina._
import org.apache.catalina.session.ManagerBase
import org.apache.catalina.util.LifecycleSupport
import org.slf4j.LoggerFactory

import scala.beans.BeanProperty

/**
 * Tomcat Session 정보를 Redis 서버에서 관리하는 Manager입니다.
 */
class RedisSessionManager extends ManagerBase with Lifecycle {

  private lazy val log = LoggerFactory.getLogger(getClass)

  protected val PREFIX = "tomcat:session:"
  protected val NULL_SESSION: RedisSession = null

  protected var handlerValve: RedisSessionHandlerValve = _
  protected var currentSession = new ThreadLocal[RedisSession]()
  protected var currentSessionId = new ThreadLocal[String]()
  protected var currentSessionIsPersisted = new ThreadLocal[Boolean]()


  @BeanProperty var host: String = "127.0.0.1"
  @BeanProperty var port: Int = 6379
  @BeanProperty var database: Int = 0

  @BeanProperty var disableListener: Boolean = false

  @BeanProperty lazy val redis: RedisSessionClient = RedisSessionClient(host, port, database)

  lazy val eventSubscriberActor = RedisSessionEventSubscriberActor(this)

  protected val lifecycle = new LifecycleSupport(this)

  override def getRejectedSessions: Int = 0

  override def load() {
    // Nothing to do.
  }

  override def unload() {
    // Nothing to do.
  }

  override def addLifecycleListener(listener: LifecycleListener) {
    lifecycle.addLifecycleListener(listener)
  }

  override def findLifecycleListeners(): Array[LifecycleListener] = {
    lifecycle.findLifecycleListeners()
  }

  override def removeLifecycleListener(listener: LifecycleListener) {
    lifecycle.removeLifecycleListener(listener)
  }

  override def startInternal(): Unit = {
    super.startInternal()

    setState(LifecycleState.STARTING)

    var attachedToValue = false
    getContext.getPipeline.getValves.toSeq.find(_.isInstanceOf[RedisSessionHandlerValve]) match {
      case Some(v) =>
        this.handlerValve = v.asInstanceOf[RedisSessionHandlerValve]
        this.handlerValve.manager = this
        log.info(s"Attached to RedisSessionHandlerValve")
        attachedToValue = true
      case None =>
    }

    if (!attachedToValue) {
      val error = "Unable to attach to session handling valve; " +
                  "sessions cannot be saved after the request without the valve starting properly."
      log.error(s"error")
      throw new LifecycleException(error)
    }

    log.info(s"Will expire session after $getMaxInactiveInterval seconds.")

    setDistributable(true)
    val path = eventSubscriberActor.path
    log.info(s"Event Listener started. path=$path")
  }

  override def stopInternal(): Unit = synchronized {
    log.debug("Stopping...")

    setState(LifecycleState.STOPPING)

    super.stopInternal()
  }

  override def createSession(sessionId: String): Session = {
    val session: RedisSession = createEmptySession().asInstanceOf[RedisSession]

    // 세션 속성값을 초기화 합니다.
    session.setNew(true)
    session.setValid(true)
    session.setCreationTime(System.currentTimeMillis())
    session.setMaxInactiveInterval(getMaxInactiveInterval)

    var jvmRoute = getJvmRoute
    var newSessionId = sessionId

    var error = true

    // 신규 세션을 생성합니다. 기존에 값이 있다면 새로 시도합니다.
    do {
      if (newSessionId == null) newSessionId = generateSessionId()
      if (jvmRoute != null)
        newSessionId += ':' + jvmRoute
    } while (!redis.setnx(newSessionId))

    /*
    Even though the key is set in Redis, we are not going to flag
    the current thread as having had the session persisted since
    the session isn't actually serialized to Redis yet.
    This ensures that the save(session) at the end of the request
    will serialize the session into Redis with 'set' instead of 'setnx'.
    */

    error = false

    session.setId(newSessionId)
    session.tellNew()

    currentSession.set(session)
    currentSessionId.set(newSessionId)
    currentSessionIsPersisted.set(false)

    session
  }

  override def createEmptySession(): Session = new RedisSession(this)

  override def add(session: Session): Unit = {
    try {
      save(session)
    } catch {
      case e: Throwable =>
        log.warn("세션을 추가하는데 실패했습니다.", e)
        throw new RuntimeException("세션을 추가하는데 실패했습니다.", e)
    }
  }

  override def findSession(id: String): Session = {
    var session: RedisSession = null
    if (id == null) {
      session = null
      currentSessionIsPersisted.set(false)
    } else if (id.equals(currentSessionId.get())) {
      session = currentSession.get()
    } else {
      session = redis.get(id)
      if (session != null) {
        session.setManager(this)
        session.setMaxInactiveInterval(getMaxInactiveInterval * 1000)
        currentSessionIsPersisted.set(true)
      }
    }
    currentSession.set(session)
    currentSessionId.set(id)

    session
  }

  def save(session: Session) {
    require(session != null)

    log.trace(s"save session into Redis...")
    val redisSession = session.asInstanceOf[RedisSession]
    val sessionIsDirty = redisSession.isDirty
    redisSession.resetDirtyTracking()

    val isCurrentSessionPersisted = currentSessionIsPersisted.get()

    if (sessionIsDirty || !isCurrentSessionPersisted) {
      redis.set(redisSession)
    }

    currentSessionIsPersisted.set(true)
    redis.expire(redisSession.getId, getMaxInactiveInterval)
  }

  override def remove(session: Session) {
    remove(session, update = false)
  }

  override def remove(session: Session, update: Boolean) {
    if (session != null) {
      log.trace(s"Remove session ${session.getId}")
      redis.del(session.getId)
    }
  }

  def afterRequest() {
    val session = currentSession.get()
    if (session != null) {
      currentSession.remove()
      currentSessionId.remove()
      currentSessionIsPersisted.remove()
      log.trace(s"Session removed from ThreadLocal. ${session.getIdInternal}")
    }
  }
  override def processExpires(): Unit = {
    // Nothing to do.
  }
}
