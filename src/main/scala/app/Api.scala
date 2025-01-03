package app

import cats.data.Kleisli
import cats.effect.kernel.Async
import cats.syntax.all.*
import dev.usommerl.BuildInfo
import eu.timepit.refined.api.Refined
import eu.timepit.refined.collection.*
import io.circe.generic.auto.*
import org.http4s.{HttpRoutes, Request, Response}
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.Location
import org.http4s.implicits.*
import org.http4s.server.middleware.CORS
import sttp.model.StatusCode
import sttp.apispec.openapi.OpenAPI
import sttp.apispec.Tag
import sttp.apispec.openapi.Info as OpenApiInfo
import sttp.apispec.openapi.Server
import sttp.apispec.openapi.circe.yaml.*
import sttp.tapir.*
import sttp.tapir.codec.refined.*
import sttp.tapir.docs.openapi.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.jsonBody
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.http4s.Http4sServerInterpreter
import sttp.tapir.swagger.SwaggerUI

object Api {
  def apply[F[_]: Async](config: ApiDocsConfig): Kleisli[F, Request[F], Response[F]] = {

    val dsl = Http4sDsl[F]
    import dsl._

    val apis: List[TapirApi[F]] = List(Examples())

    val docs: OpenAPI = OpenAPIDocsInterpreter()
      .toOpenAPI(apis.flatMap(_.endpoints), OpenApiInfo(BuildInfo.name, BuildInfo.version, description = config.description))
      .servers(List(Server(config.server.toString)))
      .tags(apis.map(_.tag))

    val swaggerUi: HttpRoutes[F]          = Http4sServerInterpreter().toRoutes(SwaggerUI[F](docs.toYaml))
    val redirectRootToDocs: HttpRoutes[F] = HttpRoutes.of[F] { case path @ GET -> Root => PermanentRedirect(Location(path.uri / "docs")) }
    val routes: List[HttpRoutes[F]]       = apis.map(_.routes) ++ List(swaggerUi, redirectRootToDocs)

    CORS.policy(routes.reduce(_ <+> _)).orNotFound
  }
}

object Examples {
  def apply[F[_]: Async]() = new TapirApi[F] {
    override val tag: Tag                                           = Tag("Getting started", None)
    override lazy val serverEndpoints: List[ServerEndpoint[Any, F]] = List(info, hello)
    type NonEmptyString = String Refined NonEmpty

    private val info: ServerEndpoint.Full[Unit, Unit, Unit, StatusCode, Info, Any, F] =
      endpoint.get
        .summary("Fetch general information about the application")
        .tag(tag.name)
        .in("info")
        .out(jsonBody[Info])
        .errorOut(statusCode)
        .serverLogic(_ =>
          Info(
            BuildInfo.name,
            BuildInfo.version,
            System.getProperty("java.vm.version"),
            BuildInfo.scalaVersion,
            BuildInfo.sbtVersion,
            BuildInfo.builtAtString,
            BuildInfo.test_libraryDependencies.sorted
          ).asRight.pure
        )

    private val hello: ServerEndpoint.Full[Unit, Unit, Option[NonEmptyString], StatusCode, String, Any, F] =
      endpoint.get
        .summary("The infamous hello world endpoint")
        .tag(tag.name)
        .in("hello")
        .in(query[Option[NonEmptyString]]("name").description("Optional name to greet"))
        .out(stringBody)
        .errorOut(statusCode)
        .serverLogic(name => s"Hello ${name.getOrElse("World")}!".asRight.pure)

    case class Info(
      name: String,
      version: String,
      vmVersion: String,
      scalaVersion: String,
      sbtVersion: String,
      builtAt: String,
      dependencies: Seq[String]
    )
  }
}

abstract class TapirApi[F[_]: Async] {
  def tag: Tag
  def serverEndpoints: List[ServerEndpoint[Any, F]]
  def endpoints: List[Endpoint[?, ?, ?, ?, ?]] = serverEndpoints.map(_.endpoint)
  def routes: HttpRoutes[F]                    = Http4sServerInterpreter().toRoutes(serverEndpoints)
}
