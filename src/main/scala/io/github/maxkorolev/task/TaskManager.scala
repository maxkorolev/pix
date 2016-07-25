package io.github.maxkorolev.task

import java.time._
import java.util.concurrent.Callable

import akka.actor.{ Actor, ActorLogging, Props }
import io.github.maxkorolev.task.TaskActor.Wait

class TaskManager[T] extends Actor with ActorLogging {

  case class Task(time: LocalDateTime, callable: Callable[T])

  def receive: Receive = {
    case Task(time, callable) => run(time, callable)
  }

  def run(time: LocalDateTime, callable: Callable[T]): Unit = {
    OffsetDateTime.now().toInstant.toEpochMilli
    run(time.atZone(ZoneId.systemDefault()).toInstant.toEpochMilli, callable)
  }

  def run(time: Long, callable: Callable[T]): Unit = {
    val name = s"task-${time.toString}"
    context.child(name) match {
      case Some(act) => run(time + 1, callable)
      case None => context.actorOf(Props(new TaskActor[T](name, callable)), name) ! Wait(time)
    }
  }

}
