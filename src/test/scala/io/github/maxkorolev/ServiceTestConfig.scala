package io.github.maxkorolev

object ServiceTestConfig {
  val config =
    """
      akka {
        loglevel = "INFO"
        actor {
          warn-about-java-serializer-usage = off
        }
        persistence {
          journal.plugin = "inmemory-journal"
          snapshot-store.plugin = "inmemory-snapshot-store"
        }
      }
    """
}
