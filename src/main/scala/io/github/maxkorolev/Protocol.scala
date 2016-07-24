package io.github.maxkorolev

import spray.json.DefaultJsonProtocol

case class Status(uptime: String)

trait Protocol extends DefaultJsonProtocol {
  implicit val statusFormatter = jsonFormat1(Status.apply)
}
