package app

import cats.arrow.FunctionK
import cats.effect.{Resource, *}
import cats.implicits.*
import cats.~>
import dev.usommerl.BuildInfo
import fs2.io.net.Network
import io.odin.*
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.{middleware, Server}

object Main extends IOApp {

  def run(args: List[String]): IO[ExitCode] =
    app.config.resource[IO].flatMap(runF[IO](_, FunctionK.id)).useForever

  def runF[F[_]: Async: Network](config: Config, functionK: F ~> IO): Resource[F, Unit] =
    for {
      logger <- makeLogger[F](config.logger, functionK)
      _      <- Resource.eval(logger.info(startMessage))
      _      <- makeServer[F](config.server)
    } yield ()

  private def makeLogger[F[_]: Async](config: LoggerConfig, functionK: F ~> IO): Resource[F, Logger[F]] =
    Resource
      .pure[F, Logger[F]](consoleLogger[F](config.formatter, config.level))
      .evalTap(logger => Sync[F].delay(OdinInterop.globalLogger.set(logger.mapK(functionK).some)))

  private def makeServer[F[_]: Async: Network](config: ServerConfig): Resource[F, Server] =
    EmberServerBuilder
      .default[F]
      .withHost(config.host)
      .withPort(config.port)
      .withHttpApp(middleware.Logger.httpApp(logHeaders = true, logBody = false)(Api[F](config.apiDocs)))
      .build

  private lazy val startMessage: String =
    "STARTED [ name: %s, version: %s, vmVersion: %s, scalaVersion: %s, sbtVersion: %s, builtAt: %s ]".format(
      BuildInfo.name,
      BuildInfo.version,
      System.getProperty("java.vm.version"),
      BuildInfo.scalaVersion,
      BuildInfo.sbtVersion,
      BuildInfo.builtAtString
    )
}
