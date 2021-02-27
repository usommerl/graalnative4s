package server

import cats.implicits._
import ciris._
import org.http4s.Uri

case class Config(port: Int, apiDocs: ApiDocsConfig)
case class ApiDocsConfig(serverUri: Uri, description: Option[String])

package object app {

  implicit private val uriConfigDecoder: ConfigDecoder[String, Uri] =
    ConfigDecoder[String, String].mapEither { case (_, v) => Uri.fromString(v).leftMap(e => ConfigError(e.details)) }

  val config: ConfigValue[Config]                                   =
    (
      env("PORT").as[Int].default(8080),
      env("APIDOCS_SERVER_URI").as[Uri].default(Uri.unsafeFromString("http://localhost:8080")),
      env("APIDOCS_DESCRIPTION").option
    ).parMapN { case (port, uri, description) => Config(port, ApiDocsConfig(uri, description)) }

}
