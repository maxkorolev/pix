package io.github.maxkorolev

import akka.actor.ActorSystem
import akka.event.{ Logging, LoggingAdapter, NoLogging }
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.persistence.inmemory.extension.{ InMemoryJournalStorage, InMemorySnapshotStorage, StorageExtension }
import akka.testkit.{ ImplicitSender, TestKit, TestProbe }
import com.typesafe.config.ConfigFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{ BeforeAndAfterAll, FlatSpecLike, Matchers }

import scala.concurrent.ExecutionContext

class ServiceTestBase extends TestKit(ActorSystem("main", ConfigFactory.parseString(ServiceTestConfig.config)))
    with FlatSpecLike with Matchers with BeforeAndAfterAll with ImplicitSender {

  protected def log: LoggingAdapter = Logging(system, "app")
  implicit lazy val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  override def afterAll {
    val probe = TestProbe()
    probe.send(StorageExtension(system).journalStorage, InMemoryJournalStorage.ClearJournal)
    probe.expectMsg(akka.actor.Status.Success(""))
    probe.send(StorageExtension(system).snapshotStorage, InMemorySnapshotStorage.ClearSnapshots)
    probe.expectMsg(akka.actor.Status.Success(""))

    super.afterAll()
    shutdown()
  }
}
