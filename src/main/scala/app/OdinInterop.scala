package app

import cats.effect.IO
import cats.effect.kernel.Sync
import cats.effect.std.Dispatcher
import cats.effect.unsafe.implicits._
import io.odin.Logger
import io.odin.slf4j.OdinLoggerBinder

import java.util.concurrent.atomic.AtomicReference

/** This implementation was stolen from: https://github.com/pitgull/pitgull/blob/v0.1.0/src/main/scala/io/pg/OdinInterop.scala
  */
class OdinInterop extends OdinLoggerBinder[IO] {

  implicit def F: Sync[IO]                = IO.asyncForIO
  implicit def dispatcher: Dispatcher[IO] = Dispatcher[IO].allocated.unsafeRunSync()._1

  val loggers: PartialFunction[String, Logger[IO]] = {
    val theLogger: String => Option[Logger[IO]] = _ => OdinInterop.globalLogger.get()
    theLogger.unlift
  }
}

object OdinInterop {
  val globalLogger = new AtomicReference[Option[Logger[IO]]](None)
}
