package server

import cats.effect.IO
import munit.CatsEffectSuite
import org.http4s.Request
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.http4s.Response
import cats.data.Kleisli
import org.http4s.headers.`Content-Type`
import org.http4s.MediaType._
import org.http4s.Status
import org.http4s.Charset
import io.circe.Json

class ApiSpec extends ApiSuite {
  test("GET /hello should greet with the name that was provided via the corresponding query parameter") {
    val response = api().run(Request[IO](method = GET, uri = uri"/hello?name=John%20Doe"))
    check(response, Ok, "Hello John Doe!")
  }

  test("GET /hello should greet the world if name parameter is omitted") {
    val response = api().run(Request[IO](method = GET, uri = uri"/hello"))
    check(response, Ok, "Hello World!")
  }
}

trait ApiSuite extends CatsEffectSuite {
  def api(serverUrl: String = "http://localhost:8080"): Kleisli[IO, Request[IO], Response[IO]] =
    Api[IO](ApiDocsConfiguration(serverUrl)).orNotFound

  def check(responseIO: IO[Response[IO]], expectedStatus: Status): IO[Unit] =
    check(responseIO, expectedStatus, None, None)

  def check(responseIO: IO[Response[IO]], expectedStatus: Status, expectedBody: Json): IO[Unit] =
    check(responseIO, expectedStatus, Some(expectedBody.noSpaces), Some(`Content-Type`(application.json)))

  def check(responseIO: IO[Response[IO]], expectedStatus: Status, expectedBody: String): IO[Unit] =
    check(responseIO, expectedStatus, Some(expectedBody), Some(`Content-Type`(text.plain, Charset.`UTF-8`)))

  def check(
    io: IO[Response[IO]],
    expectedStatus: Status,
    expectedBody: Option[String],
    expectedContentType: Option[`Content-Type`]
  ): IO[Unit] = io.flatMap { response =>
    assertEquals(response.status, expectedStatus)
    assertEquals(response.headers.get(`Content-Type`), expectedContentType)
    response.as[String].assertEquals(expectedBody.getOrElse(""))
  }
}
