package io.github.maxkorolev.base

import akka.actor.ActorSystem
import akka.event.Logging
import akka.stream.ActorMaterializer

import akka.http.scaladsl.Http
import io.github.maxkorolev.status.StatusService

object System {
  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  trait LoggerExecutor extends BaseComponent {
    protected implicit val executor = system.dispatcher
    protected implicit val log = Logging(system, "app")
  }
}

object Main extends App with Config with System.LoggerExecutor with StatusService {
  import System._

  Http().bindAndHandle(statusRoutes, httpConfig.interface, httpConfig.port)
}
