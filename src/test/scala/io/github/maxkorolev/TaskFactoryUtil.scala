package io.github.maxkorolev

import java.time.{ Instant, OffsetDateTime, ZoneId }

import io.github.maxkorolev.task.Task

import scala.concurrent.duration._

object TaskFactoryUtil {

  def exactMillis(seconds: Long): Long = {
    val time = Instant.ofEpochMilli(OffsetDateTime.now().toInstant.toEpochMilli + seconds * 1000L).atZone(ZoneId.systemDefault()).toLocalDateTime
    time.atZone(ZoneId.systemDefault()).toInstant.toEpochMilli
  }

  def successTask(seconds: Long, cb: => Unit = () => ()): Task = new Task {
    val time = exactMillis(seconds)
    val timeout = 10.seconds
    override def call(): Any = {
      Thread.sleep(1000)
    }
  }

  def failedTask(seconds: Long): Task = new Task {
    val time = exactMillis(seconds)
    val timeout = 10.seconds
    override def call(): Any = {
      Thread.sleep(1000)
      throw new RuntimeException("Something went wrong")
    }
  }

  def timeoutTask(seconds: Long): Task = new Task {
    val time = exactMillis(seconds)
    val timeout = 2.seconds
    override def call(): Any = {
      Thread.sleep(4000)
    }
  }
}
