package org.slf4j.impl

import scala.concurrent.ExecutionContext

import cats.effect.{Clock, Effect, IO, Timer}
import io.odin.{consoleLogger, Level, Logger}
import io.odin.formatter.Formatter
import io.odin.slf4j.OdinLoggerBinder

/**
  * This is bridge is needed for project dependencies that require an SLF4J API
  * See: https://github.com/valskalla/odin/tree/v0.9.1#slf4j-bridge
  *
  * This particular implementation was stolen from here:
  * https://github.com/pitgull/pitgull/blob/v0.0.2/src/main/scala/org/slf4j/impl/StaticLoggerBinder.scala
  */
class StaticLoggerBinder extends OdinLoggerBinder[IO] {
  implicit val F: Effect[IO]    = IO.ioEffect
  implicit val clock: Clock[IO] = Clock.create[IO]

  import StaticLoggerBinder.baseLogger

  val loggers: PartialFunction[String, Logger[IO]] = {
    case _ => baseLogger.withMinimalLevel(Level.Info)
  }
}

object StaticLoggerBinder extends StaticLoggerBinder {
  //EC isn't used - only Clock is required
  implicit val timer: Timer[IO]        = IO.timer(ExecutionContext.parasitic)
  val baseLogger                       = consoleLogger[IO](formatter = Formatter.colorful)
  val REQUESTED_API_VERSION: String    = "1.7"
  def getSingleton: StaticLoggerBinder = this
}
