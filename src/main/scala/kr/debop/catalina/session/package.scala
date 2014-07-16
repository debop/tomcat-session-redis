package kr.debop.catalina

/**
 * package
 * @author sunghyouk.bae@gmail.com
 */
package object session {

  implicit def using[A <: {def close() : Unit}, B](closable: A)(f: A => B): B = {
    try {
      f(closable)
    } finally {
      closable.close()
    }
  }
}
