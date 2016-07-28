package io.github.maxkorolev.task

import java.time._
import java.util.concurrent.Callable

import akka.actor.{ Actor, ActorLogging, ActorRef, Cancellable, Props }
import akka.persistence.{ PersistentActor, RecoveryCompleted, SnapshotOffer }
import io.github.maxkorolev.task.TaskActor.{ Awake, Canceled, Done, Wait }

import scala.collection.immutable.TreeSet
import scala.concurrent.duration._

object TaskManager {

  sealed trait Event

  case class TaskAdded(time: Long) extends Event
  case class TaskRun(time: Long) extends Event

  case class StartRunning(actorRef: ActorRef) extends Event
  case object StopRunning extends Event

  case class StartWaiting(cancellable: Cancellable) extends Event
  case object StopWaiting extends Event

  sealed trait Command
  case class AddTask(time: LocalDateTime, callable: Callable[Any], timeout: FiniteDuration = 10.seconds) extends Command
  case class RunTask(time: Long) extends Command

  case class TaskManagerState(
      tasks: TreeSet[Long] = TreeSet(),
      running: Option[ActorRef] = None,
      waiting: Option[Cancellable] = None
  ) {
    def pushTask(task: Long): TaskManagerState = copy(tasks = tasks + task)
    def pullTask(task: Long): TaskManagerState = copy(tasks = tasks - task)

    def startRunning(actorRef: ActorRef): TaskManagerState = copy(running = Some(actorRef))
    def stopRunning: TaskManagerState = copy(running = None)

    def startWaiting(cancellable: Cancellable): TaskManagerState = copy(waiting = Some(cancellable))
    def stopWaiting: TaskManagerState = copy(waiting = None)

    override def toString: String = tasks.toString
  }
}

class TaskManager extends PersistentActor with ActorLogging {
  import TaskManager._
  import context.dispatcher

  override def persistenceId: String = "task-manager"

  var state = TaskManagerState()

  def taskActorName(time: Long): String = s"task-${time.toString}"

  val receiveRecover: Receive = {
    case event: Long => state = state.pushTask(event)
    case SnapshotOffer(_, snapshot: TaskManagerState) => state = snapshot
  }

  val receiveCommand: Receive = {
    case AddTask(time, callable, timeout) => addTask(time, callable, timeout)
    case RunTask(time) => runTask(time)
    case Done => finishTask()
    case Canceled(_) => finishTask()
    case RecoveryCompleted => ()
  }

  def addTask(time: LocalDateTime, callable: Callable[Any], timeout: FiniteDuration): Unit = {
    OffsetDateTime.now().toInstant.toEpochMilli
    addTask(time.atZone(ZoneId.systemDefault()).toInstant.toEpochMilli, callable, timeout)
  }

  def addTask(time: Long, callable: Callable[Any], timeout: FiniteDuration): Unit = {
    if (state.tasks.contains(time)) {
      addTask(time + 1, callable, timeout)
    } else {
      context.actorOf(Props(new TaskActor(time, taskActorName(time), callable, timeout)), taskActorName(time)) ! Wait
      scheduleTask(time)
    }
  }

  def scheduleTask(task: Long): Unit = {
    state match {
      case TaskManagerState(tasks, None, Some(waiting)) if tasks.head > task =>
        waiting.cancel()
        state = state.stopWaiting
        state = state pushTask task
        scheduleFirst()
      case _ =>
        state = state pushTask task
    }
  }

  def scheduleFirst(): Unit = {
    if (state.tasks.nonEmpty) {
      val period = state.tasks.head - System.currentTimeMillis
      val cancellable = context.system.scheduler.scheduleOnce(period.millis, self, RunTask(state.tasks.head))
      state = state startWaiting cancellable
    }
  }

  def runTask(task: Long): Unit = {
    state = state pullTask task
    context child taskActorName(task) match {
      case Some(actorRef) =>
        actorRef ! Awake
        state = state startRunning actorRef
      case None =>
        scheduleFirst()
    }
  }

  def finishTask(): Unit = {
    state = state.stopRunning
  }

}
