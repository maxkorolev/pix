package io.github.maxkorolev.base

import akka.actor.ActorRef
import akka.event.LoggingAdapter

import scala.concurrent.ExecutionContext

trait BaseComponent {
  protected implicit def log: LoggingAdapter
  protected implicit def executor: ExecutionContext
  protected implicit def taskManager: ActorRef
}
