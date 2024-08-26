package app

import cats.effect.*
import cats.effect.std.Dispatcher
import dev.usommerl.BuildInfo
import fs2.io.net.Network
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.{middleware, Server}
import org.legogroup.woof.{*, given}
import org.legogroup.woof.Logger.*
import org.legogroup.woof.slf4j.*

object Main extends IOApp.Simple {

  def run: IO[Unit] = makeResources.useForever

  def makeResources: Resource[IO, Unit] =
    for
      config           <- app.config.resource[IO]
      given Logger[IO] <- makeIoLogger(config.logger)
      _                <- logStart
      _                <- makeServer[IO](config.server)
    yield ()

  private def makeIoLogger(config: LoggerConfig): Resource[IO, Logger[IO]] =
    Dispatcher.sequential[IO].flatMap { implicit dispatcher =>
      given Printer = config.printer
      given Filter  = Filter.atLeastLevel(config.level)
      for
        logger <- Resource.eval(DefaultLogger.makeIo(Output.fromConsole))
        _      <- Resource.eval(logger.registerSlf4j)
      yield logger
    }

  private def makeServer[F[_]: Async: Network](config: ServerConfig): Resource[F, Server] =
    EmberServerBuilder
      .default[F]
      .withHost(config.host)
      .withPort(config.port)
      .withHttpApp(middleware.Logger.httpApp(logHeaders = true, logBody = false)(Api[F](config.apiDocs)))
      .build

  def logStart(using logger: Logger[IO]): Resource[IO, Unit] = {
    val keys    = Set("name", "version", "scalaVersion", "sbtVersion", "builtAtString")
    val context = BuildInfo.toMap.view.filterKeys(keys.contains).mapValues(_.toString).toSeq ++ Seq(
      "vmVersion" -> System.getProperty("java.vm.version")
    )
    Resource.eval(logger.info("STARTED").withLogContext(context*))
  }
}
