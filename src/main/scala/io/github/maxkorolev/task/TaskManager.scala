package io.github.maxkorolev.task

import akka.actor.{ ActorLogging, ActorRef, Cancellable, Props }
import akka.persistence.{ PersistentActor, RecoveryCompleted, SnapshotOffer }
import io.github.maxkorolev.task.TaskActor.{ Awake, Canceled, Done, Wait }

import scala.collection.immutable.TreeMap
import scala.concurrent.duration._

object TaskManager {

  sealed trait Event

  case class TaskPushed(task: Task) extends Event
  case class TaskPulled(task: Task) extends Event

  case class StartRunning(actorRef: ActorRef) extends Event
  case object StopRunning extends Event

  sealed trait Command
  case class PushTask(task: Task) extends Command
  case class PullTask(task: Task) extends Command

  case class TaskManagerState(
    tasks: TreeMap[Long, Task] = TreeMap(),
    running: Option[ActorRef] = None,
    waiting: Option[Cancellable] = None
  )

}

class TaskManager extends PersistentActor with ActorLogging {
  import TaskManager._
  import context.dispatcher

  override def persistenceId: String = "task-manager"

  var state = TaskManagerState()

  def taskActorName(time: Long): String = s"task-${time.toString}"

  def pushTaskState(task: Task): Unit = state = state.copy(tasks = state.tasks + (task.time -> task))
  def pullTaskState(task: Task): Unit = state = state.copy(tasks = state.tasks - task.time)

  def startRunningState(actorRef: ActorRef): Unit = state = state.copy(running = Some(actorRef))
  def stopRunningState(): Unit = state = state.copy(running = None)

  def startWaitingState(waiting: Cancellable): Unit = state = state.copy(waiting = Some(waiting))
  def stopWaitingState(): Unit = state = state.copy(waiting = None)

  val receiveRecover: Receive = {
    case TaskPushed(task) => pushTaskState(task)
    case TaskPulled(task) => pullTaskState(task)
    case StartRunning(actorRef) => startRunningState(actorRef)
    case StopRunning => stopRunningState()
    case SnapshotOffer(_, snapshot: TaskManagerState) => state = snapshot
    case RecoveryCompleted =>
      log info s"RecoveryCompleted ${state.toString}"
      scheduleFirst()
  }

  val receiveCommand: Receive = {
    case PushTask(task) => addTask(task)
    case PullTask(time) => runTask(time)
    case Done =>
      stopRunningState()
      persist(StopRunning) { _ => () }
      log info s"StopRunning $state"
      scheduleFirst()
    case Canceled(_) =>
      stopRunningState()
      persist(StopRunning) { _ => () }
      log info s"StopRunning ${state.toString}"
      scheduleFirst()
  }

  def addTask(task: Task): Unit = {
    if (state.tasks.contains(task.time)) {
      addTask(task.withTime(task.time + 1))
    } else {
      context.actorOf(TaskActor.props(task, taskActorName(task.time)), taskActorName(task.time)) ! Wait
      scheduleTask(task)
    }
  }

  def scheduleTask(task: Task): Unit = {
    state -> state.tasks.headOption match {
      case (TaskManagerState(tasks, None, Some(waiting)), Some(head)) if head._1 > task.time =>
        waiting.cancel()
        stopWaitingState()
        pushTaskState(task)
        persist(TaskPushed(task)) { _ => () }
        log info s"TaskPushed ${state.toString}"
        scheduleFirst()
      case (TaskManagerState(tasks, _, _), _) =>
        pushTaskState(task)
        persist(TaskPushed(task)) { _ => () }
        log info s"TaskPushed ${state.toString}"
        scheduleFirst()
      case _ => ()
    }
  }

  def scheduleFirst(): Unit = {
    state -> state.tasks.headOption match {
      case (TaskManagerState(tasks, None, None), Some(head)) =>
        val (time, task) = head
        val period = time - System.currentTimeMillis
        val cancellable = context.system.scheduler.scheduleOnce(period.millis, self, PullTask(task))
        startWaitingState(cancellable)
      case _ => ()
    }
  }

  def runTask(task: Task): Unit = {
    val actorRef = context child taskActorName(task.time) getOrElse {
      context.actorOf(TaskActor.props(task, taskActorName(task.time)), taskActorName(task.time))
    }
    actorRef ! Awake
    pullTaskState(task)
    stopWaitingState()
    startRunningState(actorRef)
    persist(StartRunning) { _ => () }
    persist(TaskPulled(task)) { _ => () }
    log info s"TaskPulled ${state.toString}"
  }

}
