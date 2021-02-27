package server

import cats.implicits._
import ciris._
import ciris.refined._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.auto._
import eu.timepit.refined.string.Url
import eu.timepit.refined.types.net.PortNumber

import app.ServerUrl

case class Config(port: PortNumber, apiDocs: ApiDocsConfig)
case class ApiDocsConfig(serverUrl: ServerUrl, description: Option[String])

package object app {

  type ServerUrl = String Refined Url

  val config: ConfigValue[Config] =
    (
      env("PORT").as[PortNumber].default(8080),
      env("APIDOCS_SERVER_URL").as[ServerUrl].default("http://localhost:8080"),
      env("APIDOCS_DESCRIPTION").option
    ).parMapN { case (port, url, description) => Config(port, ApiDocsConfig(url, description)) }

}
