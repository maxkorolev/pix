package io.github.maxkorolev.base

import akka.actor.{ ActorSystem, Props }
import akka.event.Logging
import akka.stream.ActorMaterializer
import akka.http.scaladsl.Http
import io.github.maxkorolev.task.{ TaskManager, TaskService }

object System {
  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  trait LoggerExecutor extends BaseComponent {
    protected implicit val executor = system.dispatcher
    protected implicit val log = Logging(system, "app")
  }

  trait TaskExecutor extends BaseComponent {
    protected implicit val taskManager = system.actorOf(Props(new TaskManager), "task_manager")
  }
}

object Main extends App with Config with System.LoggerExecutor with System.TaskExecutor with TaskService {
  import System._

  Http().bindAndHandle(taskRoutes, httpConfig.interface, httpConfig.port)
}
