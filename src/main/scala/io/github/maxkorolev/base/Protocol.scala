package io.github.maxkorolev.base

import io.github.maxkorolev.status.Status
import spray.json.DefaultJsonProtocol

trait Protocol extends DefaultJsonProtocol {
  implicit val statusFormatter = jsonFormat1(Status.apply)
}
