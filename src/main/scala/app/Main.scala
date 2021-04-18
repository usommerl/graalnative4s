package app

import scala.concurrent.ExecutionContext.global

import cats.effect.{Resource, _}
import cats.implicits._
import dev.usommerl.BuildInfo
import eu.timepit.refined.auto._
import io.odin._
import org.http4s.server.{middleware, Server}
import org.http4s.server.blaze.BlazeServerBuilder

object Main extends IOApp {

  def run(args: List[String]): IO[ExitCode] =
    runF[IO].use(_ => IO.never)

  private def runF[F[_]: ContextShift: ConcurrentEffect: Timer]: Resource[F, Unit] =
    for {
      config <- app.config.resource[F]
      logger <- createLogger[F](config.logger)
      _      <- Resource.eval(logger.info(startMessage))
      _      <- serve[F](config.server)
    } yield ()

  private def createLogger[F[_]: ConcurrentEffect: Timer](config: LoggerConfig): Resource[F, Logger[F]] =
    Resource
      .pure[F, Logger[F]](consoleLogger[F](config.formatter, config.level))
      .evalTap(logger => Sync[F].delay(OdinInterop.globalLogger.set(logger.mapK(Effect.toIOK).some)))

  private def serve[F[_]: ContextShift: ConcurrentEffect: Timer](config: ServerConfig): Resource[F, Server[F]] =
    BlazeServerBuilder[F](global)
      .bindHttp(config.port, "0.0.0.0")
      .withHttpApp(middleware.Logger.httpApp(logHeaders = true, logBody = false)(Api[F](config.apiDocs)))
      .resource

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
