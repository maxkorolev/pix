package io.github.maxkorolev

import akka.actor.{ ActorRef, PoisonPill, Terminated }
import akka.testkit.TestProbe
import io.github.maxkorolev.task.TaskActor._
import io.github.maxkorolev.task.{ Task, TaskActor }

import scala.concurrent.duration._

class TaskActorTest extends TestBase {

  def createActor(task: Task): ActorRef = system.actorOf(TaskActor.props(task, s"time-${task.time}"), s"time-${task.time}")

  "TaskActor" should "successfully execute task" in {

    val task = TaskFactoryUtil.successTask(2)
    val actor = createActor(task)

    actor ! Wait

    val period = task.time - System.currentTimeMillis
    system.scheduler.scheduleOnce(period.millis, actor, Awake)
    expectMsg(10.seconds, Done)

    val probe = TestProbe()
    probe watch actor
    probe.expectTerminated(actor)

    val recreated = createActor(task)

    recreated ! GetState
    expectMsgPF() {
      case TaskState(List(Waiting, Executing, Done)) => true
    }
  }

  "TaskActor" should "cancel failed task" in {

    val task = TaskFactoryUtil.failedTask(2)
    val actor = createActor(task)

    actor ! Wait

    val period = task.time - System.currentTimeMillis
    system.scheduler.scheduleOnce(period.millis, actor, Awake)
    expectMsg(10.seconds, Canceled("Something went wrong"))

    val probe = TestProbe()
    probe watch actor
    probe.expectTerminated(actor)

    val recreated = createActor(task)

    recreated ! GetState
    expectMsgPF() {
      case TaskState(List(Waiting, Executing, Canceled(_))) => true
    }
  }

}

