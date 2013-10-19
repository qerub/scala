/* NSC -- new Scala compiler
 * Copyright 2002-2013 LAMP/EPFL
 * @author Martin Odersky
 */

package scala.tools.nsc
package reporters

import scala.reflect.internal.util._
import scala.util.DynamicVariable

/**
 * This interface provides methods to issue information, warning and
 * error messages.
 */
abstract class Reporter {
  protected def info0(pos: Position, msg: String, severity: Severity, force: Boolean): Unit

  object severity extends Enumeration
  class Severity(val id: Int) extends severity.Value {
    var count: Int = 0
  }
  val INFO    = new Severity(0) {
    override def toString: String = "INFO"
  }
  val WARNING = new Severity(1) {
    override def toString: String = "WARNING"
  }
  val ERROR   = new Severity(2) {
    override def toString: String = "ERROR"
  }

  /** Whether very long lines can be truncated.  This exists so important
   *  debugging information (like printing the classpath) is not rendered
   *  invisible due to the max message length.
   */
  private val _truncationOK = new DynamicVariable(true)
  def truncationOK = _truncationOK.value
  def withoutTruncating[T](thunk: => T) = {
    _truncationOK.withValue(false)(thunk)
  }

  private val _incompleteHandler: DynamicVariable[Option[(Position, String) => Unit]] = new DynamicVariable(None)
  def incompleteHandler = _incompleteHandler.value
  def incompleteHandled = _incompleteHandler.value.isDefined
  def withIncompleteHandler[T](handler: (Position, String) => Unit)(thunk: => T) = {
    _incompleteHandler.withValue(Some(handler))(thunk)
  }

  var cancelled   = false
  def hasErrors   = ERROR.count > 0 || cancelled
  def hasWarnings = WARNING.count > 0

  /** For sending a message which should not be labeled as a warning/error,
   *  but also shouldn't require -verbose to be visible.
   */
  def echo(msg: String): Unit                                = info(NoPosition, msg, force = true)
  def echo(pos: Position, msg: String): Unit                 = info(pos, msg, force = true)

  /** Informational messages, suppressed unless -verbose or force=true. */
  def info(pos: Position, msg: String, force: Boolean): Unit = info0(pos, msg, INFO, force)

  /** Warnings and errors. */
  def warning(pos: Position, msg: String): Unit              = withoutTruncating(info0(pos, msg, WARNING, force = false))
  def error(pos: Position, msg: String): Unit                = withoutTruncating(info0(pos, msg, ERROR, force = false))
  def incompleteInputError(pos: Position, msg: String): Unit = {
    incompleteHandler.getOrElse(error(_, _))(pos, msg)
  }

  def comment(pos: Position, msg: String) { }
  def flush() { }
  def reset() {
    INFO.count        = 0
    ERROR.count       = 0
    WARNING.count     = 0
    cancelled         = false
  }

  // sbt compat
  @deprecated("Moved to scala.reflect.internal.util.StringOps", "2.10.0")
  def countElementsAsString(n: Int, elements: String): String = StringOps.countElementsAsString(n, elements)
  @deprecated("Moved to scala.reflect.internal.util.StringOps", "2.10.0")
  def countAsString(n: Int): String = StringOps.countAsString(n)
}
