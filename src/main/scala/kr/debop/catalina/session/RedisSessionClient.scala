package kr.debop.catalina.session

import kr.debop.catalina.session.formatters.SnappyFstSessionFormatter
import org.slf4j.LoggerFactory
import redis.{ByteStringFormatter, RedisClient}

import scala.collection.JavaConverters._
import scala.concurrent._
import scala.concurrent.duration._


object RedisSessionClient {

  /** Redis 에서 다른 정보들과 구분하기 위한 PREFIX */
  val SESSION_KEY_PREFIX = "tomcat:session:"

  implicit val actorSystem = akka.actor.ActorSystem("tomcat-session")

  def apply(): RedisSessionClient = {
    new RedisSessionClient(RedisClient())
  }

  def apply(host: String, port: Int = 6379, db: Int = 0): RedisSessionClient = {
    new RedisSessionClient(RedisClient(host, port, db = Some(db)))
  }
}


/**
 * RedisSessionClient
 * @author sunghyouk.bae@gmail.com
 */
class RedisSessionClient(val redis: RedisClient) {

  private lazy val log = LoggerFactory.getLogger(getClass)

  implicit val sessionFormatter: ByteStringFormatter[RedisSession] =
    new SnappyFstSessionFormatter[RedisSession]()


  private def getRedisSessionKey(sessionId: String): String =
    RedisSessionClient.SESSION_KEY_PREFIX + sessionId


  def get(sessionId: String): RedisSession = {
    var session: RedisSession = null
    try {
      session = Await.result(redis.get[RedisSession](getRedisSessionKey(sessionId)), 5 seconds).orNull

      if (session == null) {
        log.trace(s"Session $sessionId not found in Redis.")
      } else {
        session.setId(sessionId)
        session.setNew(false)
        session.access()
        session.setValid(true)
        session.resetDirtyTracking()

        log.trace(s"Session contents [$sessionId]\n${session.getAttributeNames.asScala.mkString}")
      }
    } catch {
      case e: Throwable =>
        log.error(s"해당 세션을 Redis로부터 읽어오는데 실패했습니다. id=$sessionId", e)
    }
    session
  }

  def set(session: RedisSession) {
    if (session != null) {
      redis.set(getRedisSessionKey(session.getId), session)
      log.trace(s"Save session into Redis")
    }
  }

  def expire(sessionId: String, expires: Int) {
    redis.expire(getRedisSessionKey(sessionId), expires)
    log.trace(s"Set expiration. sessionId=$sessionId, expires=$expires")
  }


  def setnx(sessionId: String, session: RedisSession = null): Boolean =
    Await.result(redis.setnx(getRedisSessionKey(sessionId), session), 5 seconds)

  def clear() {
    val sessionKeys = keys()
    if (sessionKeys != null && sessionKeys.nonEmpty) {
      redis.del(sessionKeys: _*)
      log.info(s"Clear all sessions.")
    }
  }

  def size: Int = keys().size

  def keys(): Seq[String] = {
    Await.result(redis.keys(getRedisSessionKey("*")), 5 seconds)
  }

  def del(sessionId: String) {
    redis.del(getRedisSessionKey(sessionId))
    log.debug(s"Delete session. sessionId=$sessionId")
  }

}
