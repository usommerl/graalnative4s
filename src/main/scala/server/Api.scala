package server

import cats.Applicative
import cats.data.Kleisli
import cats.effect.{ContextShift, Sync}
import cats.implicits._
import dev.sommerlatt.BuildInfo
import org.http4s.{EntityBody, HttpRoutes, Request, Response, Uri}
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.Location
import org.http4s.implicits._
import org.http4s.server.middleware.CORS
import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.docs.openapi._
import sttp.tapir.openapi.{OpenAPI, Server, Tag}
import sttp.tapir.openapi.circe.yaml._
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.http4s._
import sttp.tapir.swagger.http4s.SwaggerHttp4s

object Api {
  def apply[F[_]: Sync: ContextShift](config: ApiDocsConfiguration): Kleisli[F, Request[F], Response[F]] = {

    val dsl = Http4sDsl[F]
    import dsl._

    val apis: List[TapirApi[F]] = List(HelloWorldApi())

    val docs: OpenAPI = apis
      .flatMap(_.endpoints)
      .toOpenAPI(BuildInfo.name, BuildInfo.version)
      .servers(List(Server(config.serverUrl)))
      .tags(apis.map(_.tag))

    val redirectRootToDocs =
      HttpRoutes.of[F] {
        case path @ GET -> Root =>
          Uri
            .fromString(s"${path.uri}/docs")
            .map(uri => PermanentRedirect(Location(uri)))
            .getOrElse(NotFound())
      }

    val routes: List[HttpRoutes[F]] =
      apis.map(_.routes) ++ List(new SwaggerHttp4s(docs.toYaml).routes, redirectRootToDocs)

    CORS(routes.reduce(_ <+> _)).orNotFound
  }
}

object HelloWorldApi {
  def apply[F[_]: Sync: ContextShift]()(implicit F: Applicative[F]) = new TapirApi[F] {

    override val tag                  = Tag("Getting started", None)
    override lazy val serverEndpoints = List(get)

    private val get: ServerEndpoint[Option[String], StatusCode, String, Nothing, F] =
      endpoint.get
        .summary("The infamous hello world endpoint")
        .tag(tag.name)
        .in("hello")
        .in(query[Option[String]]("name").description("Optional name to greet"))
        .out(stringBody)
        .errorOut(statusCode)
        .serverLogic(name => F.pure(s"Hello ${name.getOrElse("World")}!".asRight))
  }
}

abstract class TapirApi[F[_]: Sync: ContextShift] {
  def tag: Tag
  def serverEndpoints: List[ServerEndpoint[_, _, _, EntityBody[F], F]]
  def endpoints: List[Endpoint[_, _, _, _]] = serverEndpoints.map(_.endpoint)
  def routes: HttpRoutes[F]                 = serverEndpoints.toRoutes
}
