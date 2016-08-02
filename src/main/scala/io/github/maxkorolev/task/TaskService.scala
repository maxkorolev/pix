package io.github.maxkorolev.task

import java.time.{ Instant, LocalDateTime, OffsetDateTime, ZoneId }
import java.util.concurrent.Callable

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import io.github.maxkorolev.base.BaseService
import io.github.maxkorolev.task.TaskManager.PushTask

import scala.concurrent.duration._

case class TaskInput(time: Long)

trait TaskService extends BaseService {

  protected val taskRoutes =
    pathPrefix("task") {
      post {
        entity(as[TaskInput]) { in =>
          log.info("/task executed")

          val time = Instant.ofEpochMilli(OffsetDateTime.now().toInstant.toEpochMilli + in.time * 1000L).atZone(ZoneId.systemDefault()).toLocalDateTime

          val millis = time.atZone(ZoneId.systemDefault()).toInstant.toEpochMilli
          val task = new Task {
            val time = millis
            val timeout = 10.seconds
            override def call(): Any = {
              Thread.sleep(3000)
            }
          }
          taskManager ! PushTask(task)

          complete(StatusCodes.OK)
        }
      }
    }
}
