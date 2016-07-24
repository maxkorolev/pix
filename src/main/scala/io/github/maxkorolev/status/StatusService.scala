package io.github.maxkorolev.status

import java.lang.management.ManagementFactory

import akka.http.scaladsl.server.Directives._
import io.github.maxkorolev.base.BaseService

import scala.concurrent.duration._

trait StatusService extends BaseService {
  protected val statusRoutes = pathPrefix("status") {
    get {
      log.info("/status executed")
      complete(Status(Duration(ManagementFactory.getRuntimeMXBean.getUptime, MILLISECONDS).toString()))
    }
  }
}
