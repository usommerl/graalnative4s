package app

import cats.implicits.*
import ciris.{Effect, *}
import com.comcast.ip4s.{Host, Port}
import io.odin.Level
import io.odin.formatter.Formatter
import io.odin.json.{Formatter => JFormatter}
import org.http4s.Uri
import org.http4s.implicits.*

case class Config(server: ServerConfig, logger: LoggerConfig)
case class ServerConfig(host: Host, port: Port, apiDocs: ApiDocsConfig)
case class ApiDocsConfig(server: Uri, description: Option[String])
case class LoggerConfig(level: Level, formatter: Formatter)

package object app {

  val config: ConfigValue[Effect, Config] = (
    serverConfig,
    loggerConfig
  ).parMapN(Config.apply)

  private lazy val serverConfig: ConfigValue[Effect, ServerConfig] = (
    env("HOST").as[Host].default(Host.fromString("0.0.0.0").get),
    env("PORT").as[Port].default(Port.fromInt(8080).get),
    apiDocsConfig
  ).parMapN(ServerConfig.apply)

  private lazy val apiDocsConfig: ConfigValue[Effect, ApiDocsConfig] = (
    env("APIDOCS_SERVER_URL").as[Uri].default(uri"http://localhost:8080"),
    env("APIDOCS_DESCRIPTION").option
  ).parMapN(ApiDocsConfig.apply)

  private lazy val loggerConfig: ConfigValue[Effect, LoggerConfig] = (
    env("LOG_LEVEL").as[Level].default(Level.Info),
    env("LOG_FORMATTER").as[Formatter].default(Formatter.colorful)
  ).parMapN(LoggerConfig.apply)

  private implicit lazy val portDecoder: ConfigDecoder[String, Port] =
    ConfigDecoder[String, String].mapOption("Port")(Port.fromString)

  private implicit lazy val hostDecoder: ConfigDecoder[String, Host] =
    ConfigDecoder[String, String].mapOption("Host")(Host.fromString)

  private implicit lazy val uriDecoder: ConfigDecoder[String, Uri] =
    ConfigDecoder[String, String].mapOption("Uri")(Uri.fromString(_).toOption)

  private implicit lazy val logLevelDecoder: ConfigDecoder[String, Level] =
    ConfigDecoder[String, String].mapOption("Level")(_.toLowerCase match {
      case "trace" => Level.Trace.some
      case "debug" => Level.Debug.some
      case "info"  => Level.Info.some
      case "warn"  => Level.Warn.some
      case "error" => Level.Error.some
      case _       => None
    })

  private implicit lazy val logFormatterDecoder: ConfigDecoder[String, Formatter] =
    ConfigDecoder[String, String].mapOption("Formatter")(_.toLowerCase match {
      case "default"  => Formatter.default.some
      case "colorful" => Formatter.colorful.some
      case "json"     => JFormatter.json.some
      case _          => None
    })
}
