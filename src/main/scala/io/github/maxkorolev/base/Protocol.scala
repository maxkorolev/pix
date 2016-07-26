package io.github.maxkorolev.base

import io.github.maxkorolev.task.TaskInput
import spray.json.DefaultJsonProtocol

trait Protocol extends DefaultJsonProtocol {
  implicit val taskInputFormatter = jsonFormat1(TaskInput.apply)
}
