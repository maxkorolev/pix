package io.github.maxkorolev.task

import akka.actor.ActorSystem

class TaskManager[T](actorSystem: ActorSystem) {

  def run(time: Long, task: => T): Unit = {

  }

}
