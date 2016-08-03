package io.github.maxkorolev.task

import java.util.concurrent.Callable

import scala.concurrent.duration.FiniteDuration

trait Task extends Callable[Any] with Serializable {
  val time: Long
  val timeout: FiniteDuration

  def withTime(t: Long): Task = new Task {
    val time: Long = t
    val timeout: FiniteDuration = timeout

    override def call(): AnyRef = call _
  }

  override def equals(any: Any): Boolean = {
    any match {
      case t: Task => time == t.time && timeout == t.timeout
      case _ => false
    }
  }

  override def hashCode: Int = {
    val prime = 31
    var result = 1
    result = prime * result + time.toInt
    result = prime * result + timeout.hashCode()
    result
  }

  override def toString: String = s"Task(time: $time, timeout: ${timeout.toString})"
}

