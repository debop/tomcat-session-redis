package kr.debop.catalina.session


import java.security.Principal
import java.util
import java.util.Objects
import java.util.concurrent.ConcurrentHashMap

import org.apache.catalina.session.StandardSession
import org.apache.catalina.{Manager, SessionListener}
import org.slf4j.LoggerFactory

import scala.beans.BeanProperty
import scala.collection.mutable

/**
 * Redis 에 저장할 Session 정보
 * @author sunghyouk.bae@gmail.com
 */
class RedisSession(private[this] val manager: Manager) extends StandardSession(manager) {

  resetDirtyTracking()

  private lazy val log = LoggerFactory.getLogger(getClass)

  @BeanProperty var manualDirtyTrackingSupportEnabled = false

  @BeanProperty protected var manualDirtyTrackingAttributeKey = "__changed__"

  protected var changedAttributes = mutable.HashMap[String, Any]()
  protected var dirty: Boolean = false

  def isDirty: Boolean = dirty || changedAttributes.nonEmpty

  def resetDirtyTracking(): Unit = {
    changedAttributes = new mutable.HashMap[String, Any]()
    dirty = false
    notes = new util.Hashtable[String, Object]()
    attributes = new ConcurrentHashMap[String, Object]()
    listeners = new util.ArrayList[SessionListener]()
  }

  override def setAttribute(name: String, value: Any): Unit = {
    if (manualDirtyTrackingSupportEnabled && manualDirtyTrackingAttributeKey.equals(name)) {
      dirty = true
    }

    val oldValue = getAttribute(name)
    val isSame = Objects.equals(value, oldValue)
    if (!isSame) {
      changedAttributes.put(name, value)
    }
    super.setAttribute(name, value)
  }

  override def removeAttribute(name: String): Unit = {
    dirty = true
    super.removeAttribute(name)
  }

  override def setId(id: String): Unit = {
    // Specifically do not call super(): it's implementation does unexpected things
    // like calling manager.remove(session.id) and manager.add(session).
    this.id = id
  }

  override def setPrincipal(principal: Principal): Unit = {
    dirty = true
    super.setPrincipal(principal)
  }
}
