package server

import scala.concurrent.ExecutionContext.global

import cats.effect._
import cats.implicits._
import dev.usommerl.BuildInfo
import io.odin._
import org.http4s.HttpApp
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.{Logger => Log}
import org.slf4j.impl.StaticLoggerBinder
import pureconfig.ConfigSource
import pureconfig.generic.auto._
import pureconfig.module.catseffect.syntax._

object Main extends IOApp {

  def run(args: List[String]): IO[ExitCode] = {
    implicit val logger = StaticLoggerBinder.baseLogger.withMinimalLevel(Level.Info)
    runF[IO]
  }

  private def runF[F[_]: Sync: ContextShift: ConcurrentEffect: Timer: Logger]: F[ExitCode] =
    for {
      _        <- Logger[F].info(startMessage)
      config   <- Blocker[F].use(ConfigSource.default.loadF[F, Configuration])
      httpApp   = Log.httpApp(logHeaders = true, logBody = false)(Api[F](config.apiDocs))
      exitCode <- serve[F](config.port, httpApp).as(ExitCode.Success)
    } yield exitCode

  private def serve[F[_]: ConcurrentEffect: Timer](port: Int, httpApp: HttpApp[F]): F[Unit] =
    BlazeServerBuilder[F](global)
      .bindHttp(port, "0.0.0.0")
      .withHttpApp(httpApp)
      .serve
      .compile
      .drain

  private lazy val startMessage: String =
    "STARTED [ name: %s, version: %s, scalaVersion: %s, sbtVersion: %s, builtAt: %s ]".format(
      BuildInfo.name,
      BuildInfo.version,
      BuildInfo.scalaVersion,
      BuildInfo.sbtVersion,
      BuildInfo.builtAtString
    )
}
