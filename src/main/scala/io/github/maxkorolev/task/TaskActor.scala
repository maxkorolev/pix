package io.github.maxkorolev.task

import akka.actor.ActorLogging
import akka.persistence.{ PersistentActor, SnapshotOffer }

import scala.concurrent.Future

object TaskActor {

  trait Command
  case object Wait extends Command
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

class TaskActor[T](time: Long, task: => T) extends PersistentActor with ActorLogging {

  import TaskActor._
  import akka.pattern.pipe
  import context.dispatcher

  override def persistenceId: String = s"task-${time.toString}"

  var state = TaskState()

  def updateState(event: Event): Unit = state = state.updated(event)

  val receiveRecover: Receive = {
    case event: Event => updateState(event)
    case SnapshotOffer(_, snapshot: TaskState) => state = snapshot
  }

  val receiveCommand: Receive = {
    case Wait =>
      persist(Waiting)(updateState)

    case Awake =>
      Future { task } map { _ => Finish } recover { case err => Cancel(err.getMessage) } pipeTo sender
      persist(Executing)(updateState)

    case Finish =>
      persist(Done)(updateState)

    case Cancel(err) =>
      persist(Canceled(err))(updateState)
  }
}

