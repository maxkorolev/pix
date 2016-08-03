package io.github.maxkorolev

import akka.actor.ActorSystem
import akka.event.{ Logging, LoggingAdapter }
import akka.persistence.inmemory.extension.{ InMemoryJournalStorage, InMemorySnapshotStorage, StorageExtension }
import akka.testkit.{ ImplicitSender, TestKit, TestProbe }
import com.typesafe.config.ConfigFactory
import org.scalatest.{ BeforeAndAfterAll, BeforeAndAfterEach, FlatSpecLike, Matchers }

import scala.concurrent.ExecutionContext

class TestBase extends TestKit(ActorSystem("main", ConfigFactory.parseString(TestConfig.config)))
    with FlatSpecLike with Matchers with BeforeAndAfterEach with BeforeAndAfterAll with ImplicitSender {

  protected def log: LoggingAdapter = Logging(system, "app")
  implicit lazy val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  override def afterEach {
    val probe = TestProbe()
    probe.send(StorageExtension(system).journalStorage, InMemoryJournalStorage.ClearJournal)
    probe.expectMsg(akka.actor.Status.Success(""))
    probe.send(StorageExtension(system).snapshotStorage, InMemorySnapshotStorage.ClearSnapshots)
    probe.expectMsg(akka.actor.Status.Success(""))
    super.afterEach()
  }

  override def afterAll {
    super.afterAll()
    shutdown()
  }
}
