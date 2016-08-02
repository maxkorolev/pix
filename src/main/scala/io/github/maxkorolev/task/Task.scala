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

  override def toString: String = s"Task(time: $time, timeout: ${timeout.toString})"
}

