package io.github.maxkorolev.task

import java.time.{ Instant, LocalDateTime, ZoneId }
import java.util.concurrent.Callable

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import io.github.maxkorolev.base.BaseService
import io.github.maxkorolev.task.TaskManager.Task

import scala.concurrent.duration._

case class TaskInput(time: Long)

trait TaskService extends BaseService {

  protected val taskRoutes =
    pathPrefix("task") {
      post {
        entity(as[TaskInput]) { in =>
          log.info("/task executed")

          val time = Instant.ofEpochMilli(in.time).atZone(ZoneId.systemDefault()).toLocalDateTime

          taskManager ! Task(time, new Callable[Int] {
            override def call(): Int = {
              throw new RuntimeException("----------------")
              5
            }
          })

          complete(StatusCodes.OK)
        }
      }
    }
}
