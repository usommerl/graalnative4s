package app

import scala.util.Try

import cats.syntax.all.*
import ciris.{Effect, *}
import com.comcast.ip4s.{Host, Port}
import org.http4s.Uri
import org.http4s.implicits.*
import org.legogroup.woof.{ColorPrinter, JsonPrinter, LogLevel, NoColorPrinter, Printer}
import org.legogroup.woof.LogLevel.Info

case class Config(server: ServerConfig, logger: LoggerConfig)
case class ServerConfig(host: Host, port: Port, apiDocs: ApiDocsConfig)
case class ApiDocsConfig(server: Uri, description: Option[String])
case class LoggerConfig(level: LogLevel, printer: Printer)

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
    env("LOG_LEVEL").as[LogLevel].default(Info),
    env("LOG_FORMAT").as[Printer].default(ColorPrinter())
  ).parMapN(LoggerConfig.apply)

  given ConfigDecoder[String, Port]     = ConfigDecoder[String, String].mapOption("Port")(Port.fromString)
  given ConfigDecoder[String, Host]     = ConfigDecoder[String, String].mapOption("Host")(Host.fromString)
  given ConfigDecoder[String, Uri]      = ConfigDecoder[String, String].mapOption("Uri")(Uri.fromString(_).toOption)
  given ConfigDecoder[String, LogLevel] =
    ConfigDecoder[String, String].mapOption("LogLevel")(s => Try(LogLevel.valueOf(s.toLowerCase.capitalize)).toOption)

  given ConfigDecoder[String, Printer] =
    ConfigDecoder[String, String].mapOption("Printer")(_.toLowerCase match {
      case "nocolor" => NoColorPrinter().some
      case "color"   => ColorPrinter().some
      case "json"    => JsonPrinter().some
      case _         => None
    })
}
