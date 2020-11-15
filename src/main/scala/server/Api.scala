package server

import cats.Applicative
import cats.data.Kleisli
import cats.effect.{ContextShift, Sync}
import cats.implicits._
import dev.usommerl.BuildInfo
import io.circe.generic.auto._
import org.http4s.{EntityBody, HttpRoutes, Request, Response}
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.Location
import org.http4s.implicits._
import org.http4s.server.middleware.CORS
import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.docs.openapi._
import sttp.tapir.json.circe.jsonBody
import sttp.tapir.openapi.{OpenAPI, Server, Tag}
import sttp.tapir.openapi.circe.yaml._
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.http4s._
import sttp.tapir.swagger.http4s.SwaggerHttp4s

object Api {
  def apply[F[_]: Sync: ContextShift](config: ApiDocsConfiguration): Kleisli[F, Request[F], Response[F]] = {

    val dsl = Http4sDsl[F]
    import dsl._

    val apis: List[TapirApi[F]] = List(Examples())

    val docs: OpenAPI = apis
      .flatMap(_.endpoints)
      .toOpenAPI(openapi.Info(BuildInfo.name, BuildInfo.version, config.description))
      .servers(List(Server(config.serverUrl)))
      .tags(apis.map(_.tag))

    val redirectRootToDocs = HttpRoutes.of[F] { case path @ GET -> Root => PermanentRedirect(Location(path.uri / "docs")) }

    val routes: List[HttpRoutes[F]] = apis.map(_.routes) ++ List(new SwaggerHttp4s(docs.toYaml).routes, redirectRootToDocs)

    CORS(routes.reduce(_ <+> _)).orNotFound
  }
}

object Examples {
  def apply[F[_]: Sync: ContextShift]()(implicit F: Applicative[F]) = new TapirApi[F] {
    override val tag                  = Tag("Getting started", None)
    override lazy val serverEndpoints = List(info, hello)

    private val info: ServerEndpoint[Unit, StatusCode, Info, Nothing, F] =
      endpoint.get
        .summary("Fetch general information about the application")
        .tag(tag.name)
        .in("info")
        .out(jsonBody[Info])
        .errorOut(statusCode)
        .serverLogic(_ =>
          F.pure(
            Info(
              BuildInfo.name,
              BuildInfo.version,
              BuildInfo.scalaVersion,
              BuildInfo.sbtVersion,
              BuildInfo.builtAtString,
              BuildInfo.test_libraryDependencies.sorted
            ).asRight
          )
        )

    private val hello: ServerEndpoint[Option[String], StatusCode, String, Nothing, F] =
      endpoint.get
        .summary("The infamous hello world endpoint")
        .tag(tag.name)
        .in("hello")
        .in(query[Option[String]]("name").description("Optional name to greet"))
        .out(stringBody)
        .errorOut(statusCode)
        .serverLogic(name => F.pure(s"Hello ${name.getOrElse("World")}!".asRight))

    case class Info(
      name: String,
      version: String,
      scalaVersion: String,
      sbtVersion: String,
      builtAt: String,
      dependencies: Seq[String]
    )
  }
}

abstract class TapirApi[F[_]: Sync: ContextShift] {
  def tag: Tag
  def serverEndpoints: List[ServerEndpoint[_, _, _, EntityBody[F], F]]
  def endpoints: List[Endpoint[_, _, _, _]] = serverEndpoints.map(_.endpoint)
  def routes: HttpRoutes[F]                 = serverEndpoints.toRoutes
}
