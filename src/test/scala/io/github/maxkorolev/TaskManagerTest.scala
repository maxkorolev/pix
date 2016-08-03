package io.github.maxkorolev

import akka.actor.{ ActorRef, PoisonPill, Props }
import akka.testkit.TestProbe
import io.github.maxkorolev.task.TaskManager._
import io.github.maxkorolev.task.{ TaskManager, Task }

import scala.collection.immutable.TreeMap
import scala.concurrent.duration._

class TaskManagerTest extends TestBase {

  def createActor: ActorRef = system.actorOf(Props(new TaskManager), "task_manager")

  "TaskManager" should "successfully execute task" in {

    val firstTask = TaskFactoryUtil.successTask(1)
    val secondTask = TaskFactoryUtil.successTask(1)
    val actor = createActor

    actor ! PushTask(firstTask)
    actor ! PushTask(secondTask)

    val shouldTasks = List(
      firstTask.time -> firstTask,
      secondTask.time -> secondTask
    )

    actor ! GetState
    expectMsgPF() {
      case TaskManagerState(tasks, None, Some(_)) if tasks.toList == shouldTasks => true
    }

    Thread.sleep(5000)

    actor ! GetState
    expectMsgPF() {
      case TaskManagerState(tasks, None, None) if tasks.isEmpty => true
    }

    actor ! PoisonPill
  }

  "TaskManager" should "keep order" in {

    var counter = 0
    val firstTask = TaskFactoryUtil.successTask(3, {
      counter shouldBe 1
      counter = counter + 1
    })
    val secondTask = TaskFactoryUtil.successTask(1, {
      counter shouldBe 0
      counter = counter + 1
    })
    val actor = createActor

    actor ! PushTask(firstTask)
    actor ! PushTask(secondTask)

    val shouldTasks = List(
      secondTask.time -> secondTask,
      firstTask.time -> firstTask
    )

    actor ! GetState
    expectMsgPF() {
      case TaskManagerState(tasks, None, Some(_)) if tasks.toList == shouldTasks => true
    }

    Thread.sleep(5000)

    actor ! GetState
    expectMsgPF() {
      case TaskManagerState(tasks, None, None) if tasks.isEmpty => true
    }

    actor ! PoisonPill
  }

  "TaskManager" should "be able to die" in {

    var counter = 0
    val firstTask = TaskFactoryUtil.successTask(5, {
      counter shouldBe 1
      counter = counter + 1
    })
    val secondTask = TaskFactoryUtil.successTask(4, {
      counter shouldBe 0
      counter = counter + 1
    })
    val actor = createActor

    actor ! PushTask(firstTask)
    actor ! PushTask(secondTask)

    Thread.sleep(1000)
    val probe = TestProbe()
    probe watch actor
    actor ! PoisonPill
    probe.expectTerminated(actor)

    val recreated = createActor
    recreated ! GetState

    val shouldTasks = List(
      secondTask.time -> secondTask,
      firstTask.time -> firstTask
    )
    expectMsgPF() {
      case TaskManagerState(tasks, None, Some(_)) if tasks.toList == shouldTasks => true
    }

    Thread.sleep(10000)

    recreated ! GetState
    expectMsgPF() {
      case TaskManagerState(tasks, None, None) if tasks.isEmpty => true
    }

    recreated ! PoisonPill
  }
}

