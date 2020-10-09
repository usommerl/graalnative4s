package server

import scala.concurrent.ExecutionContext.global

import cats.implicits._
import cats.effect._
import dev.sommerlatt.BuildInfo
import io.odin._
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.{Logger => Http4sLogger}
import org.slf4j.impl.StaticLoggerBinder
import pureconfig.ConfigSource
import pureconfig.generic.auto._
import pureconfig.module.catseffect.syntax._
import org.http4s.HttpApp

object Main extends IOApp {

  def run(args: List[String]): IO[ExitCode] = {
    implicit val logger: Logger[IO] = StaticLoggerBinder.baseLogger.withMinimalLevel(Level.Info)
    runF[IO]
  }

  def runF[F[_]: Sync: ContextShift: ConcurrentEffect: Timer: Logger]: F[ExitCode] =
    for {
      _        <- Logger[F].info(s"STARTED  [ $BuildInfo ]")
      config   <- Blocker[F].use(ConfigSource.default.loadF[F, Configuration])
      api      = Api[F](config.apiDocs).orNotFound
      httpApp  = Http4sLogger.httpApp(logHeaders = true, logBody = false)(api)
      exitCode <- serve[F](httpApp).as(ExitCode.Success)
    } yield exitCode

  def serve[F[_]: ConcurrentEffect: Timer](httpApp: HttpApp[F]): F[Unit] =
    BlazeServerBuilder[F](global)
      .bindHttp(8080, "0.0.0.0")
      .withHttpApp(httpApp)
      .serve
      .compile
      .drain
}
