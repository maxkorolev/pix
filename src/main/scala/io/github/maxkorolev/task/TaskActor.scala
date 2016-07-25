package io.github.maxkorolev.task

import java.util.concurrent.Callable

import akka.actor.ActorLogging
import akka.persistence.{ PersistentActor, SnapshotOffer }

import scala.concurrent.Future
import scala.concurrent.duration._

object TaskActor {

  trait Command
  case class Wait(time: Long) extends Command
  case object Awake extends Command
  case object Finish extends Command
  case class Cancel(err: String) extends Command

  trait Event
  case object Waiting extends Event
  case object Executing extends Event
  case object Done extends Event
  case class Canceled(err: String) extends Event

  case class TaskState(events: List[Event] = Nil) {
    def updated(event: Event): TaskState = copy(event :: events)
    def size: Int = events.length
    override def toString: String = events.reverse.toString
  }

}

class TaskActor[T](name: String, callable: Callable[T]) extends PersistentActor with ActorLogging {

  import TaskActor._
  import akka.pattern.pipe
  import context.dispatcher

  override def persistenceId: String = name

  var state = TaskState()

  def updateState(event: Event): Unit = {
    state = state.updated(event)
  }

  def updateSnapshot(event: Event): Unit = {
    updateState(event)
    saveSnapshot(state)
  }

  val receiveRecover: Receive = {
    case event: Event => updateState(event)
    case SnapshotOffer(_, snapshot: TaskState) => state = snapshot
  }

  val receiveCommand: Receive = {
    case Wait(time) =>
      val period = time - System.currentTimeMillis
      context.system.scheduler.scheduleOnce(period.millis, self, Awake)
      persist(Waiting)(updateState)

    case Awake =>
      Future { callable.call() } map { _ => Finish } recover { case err => Cancel(err.getMessage) } pipeTo self
      persist(Executing)(updateState)

    case Finish =>
      persist(Done)(updateSnapshot)

    case Cancel(err) =>
      persist(Canceled(err))(updateSnapshot)
  }
}

