package io.github.maxkorolev.task

import java.time.format.DateTimeFormatter
import java.time.{ Instant, ZoneId }
import java.util.concurrent.Callable

import akka.actor.{ ActorLogging, ActorRef, Props, ReceiveTimeout }
import akka.persistence.{ PersistentActor, SaveSnapshotFailure, SaveSnapshotSuccess, SnapshotOffer }

import scala.concurrent.Future
import scala.concurrent.duration._

object TaskActor {

  trait Command
  case object Wait extends Command
  case object Awake extends Command
  case object Finish extends Command
  case object GetState extends Command
  case class Cancel(err: String) extends Command

  trait Event
  case object Waiting extends Event
  case object Executing extends Event
  case object Done extends Event
  case class Canceled(err: String) extends Event

  case class TaskState(events: List[Event] = Nil) {
    def updated(event: Event): TaskState = copy(events :+ event)
  }

  def props(task: Task, name: String): Props = Props(new TaskActor(task.time, name, task.call, task.timeout))
}

class TaskActor(time: Long, name: String, call: () => Any, timeout: FiniteDuration) extends PersistentActor with ActorLogging {
  import TaskActor._
  import akka.pattern.pipe
  import context.dispatcher

  override def persistenceId: String = name

  var state = TaskState()

  def updateState(event: Event): Unit = {
    state = state.updated(event)
  }

  def updateSnapshot(manager: ActorRef)(event: Event): Unit = {
    updateState(event)
    saveSnapshot(state)
  }

  def working(manager: ActorRef): Receive = {
    case Finish =>
      log info s"Task has been finished successful"
      persist(Done)(updateSnapshot(manager))
      manager ! Done
      context become receiveCommand

    case Cancel(err) =>
      log info s"Task has been canceled because of $err"
      persist(Canceled(err))(updateSnapshot(manager))
      manager ! Canceled(err)
      context become receiveCommand

    case ReceiveTimeout =>
      log info s"Task has been canceled because of timeout"
      persist(Canceled("Timeout"))(updateSnapshot(manager))
      manager ! Canceled("Timeout")
      context become receiveCommand

    case GetState =>
      sender ! state
  }

  def sleeping: Receive = {
    case Awake =>
      log info s"Task will be executed now"
      context setReceiveTimeout timeout
      Future { call() } map { _ => Finish } recover { case err => Cancel(err.getMessage) } pipeTo self
      persist(Executing)(updateState)
      context become working(sender)

    case GetState =>
      sender ! state
  }

  val receiveCommand: Receive = {
    case Wait =>
      val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
      val datetime = Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault())
      log info s"Task will be executed in a ${datetime format formatter}"
      persist(Waiting)(updateState)
      context become sleeping

    case GetState =>
      sender ! state

    case SaveSnapshotSuccess(_) =>
      context stop self

    case SaveSnapshotFailure(metadata, reason) =>
      log info s"Snapshot couldn't be stored because of ${reason.getMessage}"
      context stop self
  }

  val receiveRecover: Receive = {
    case event: Event => updateState(event)
    case SnapshotOffer(_, snapshot: TaskState) => state = snapshot
  }
}

