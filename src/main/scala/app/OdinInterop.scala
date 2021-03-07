package app

import java.util.concurrent.atomic.AtomicReference

import cats.effect.Clock
import cats.effect.Effect
import cats.effect.IO
import io.odin.Logger
import io.odin.slf4j.OdinLoggerBinder

/** This implementation was stolen from here:
  * https://github.com/pitgull/pitgull/blob/v0.0.7/src/main/scala/io/pg/OdinInterop.scala
  */
class OdinInterop extends OdinLoggerBinder[IO] {
  implicit val F: Effect[IO]    = IO.ioEffect
  implicit val clock: Clock[IO] = Clock.create[IO]

  val loggers: PartialFunction[String, Logger[IO]] = {
    val theLogger: String => Option[Logger[IO]] = _ => OdinInterop.globalLogger.get()
    theLogger.unlift
  }
}

object OdinInterop {
  val globalLogger = new AtomicReference[Option[Logger[IO]]](None)
}
