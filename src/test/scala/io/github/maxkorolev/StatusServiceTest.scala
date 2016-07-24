package io.github.maxkorolev

import akka.http.scaladsl.model.StatusCodes
import io.github.maxkorolev.status.{ Status, StatusService }

class StatusServiceTest extends ServiceTestBase with StatusService {
  "StatusService" when {
    "GET /status" should {
      "return time" in {
        Get("/status") ~> statusRoutes ~> check {
          status should be(StatusCodes.OK)
          responseAs[Status].uptime should include("milliseconds")
        }
      }
    }
  }
}
