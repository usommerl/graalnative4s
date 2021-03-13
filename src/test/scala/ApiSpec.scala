package app

import cats.data.Kleisli
import cats.effect.IO
import dev.usommerl.BuildInfo
import eu.timepit.refined.auto._
import io.circe.Json
import io.circe.literal._
import munit.CatsEffectSuite
import org.http4s.{Charset, Request, Response, Status}
import org.http4s.MediaType._
import org.http4s.dsl.io._
import org.http4s.headers.{`Content-Type`, `Location`}
import org.http4s.implicits._

import app.ServerUrl

class ApiSpec extends ApiSuite {

  test("GET /hello should greet the world if name parameter is omitted") {
    val response = api().run(Request[IO](method = GET, uri = uri"/hello"))
    check(response, Ok, "Hello World!")
  }

  test("GET /hello should greet with the name that was provided via the corresponding query parameter") {
    val response = api().run(Request[IO](method = GET, uri = uri"/hello?name=John%20Doe"))
    check(response, Ok, "Hello John Doe!")
  }

  test("GET /hello not accept empty string as name parameter") {
    val response = api().run(Request[IO](method = GET, uri = uri"/hello?name="))
    check(response, BadRequest, evaluateBody = false)
  }

  test("GET /info should respond with application information") {
    val response = api().run(Request[IO](method = GET, uri = uri"/info"))
    check(
      response,
      Ok,
      json"""
      {
        "name": ${BuildInfo.name},
        "version": ${BuildInfo.version},
        "vmVersion": ${System.getProperty("java.vm.version")},
        "scalaVersion": ${BuildInfo.scalaVersion},
        "sbtVersion": ${BuildInfo.sbtVersion},
        "builtAt": ${BuildInfo.builtAtString},
        "dependencies": ${BuildInfo.test_libraryDependencies.sorted}
      }"""
    )
  }

  test("GET / should redirect to /docs endpoint") {
    val response = api().run(Request[IO](method = GET, uri = uri"/"))
    check(response, PermanentRedirect)
    response.map(r => assertEquals(r.headers.get(`Location`), Some(Location(uri"/docs"))))
  }
}

trait ApiSuite extends CatsEffectSuite {
  def api(serverUrl: ServerUrl = "http://localhost:8080"): Kleisli[IO, Request[IO], Response[IO]] =
    Api[IO](ApiDocsConfig(serverUrl, None))

  def check(responseIO: IO[Response[IO]], expectedStatus: Status, expectedBody: Json): IO[Unit] =
    check(responseIO, expectedStatus, Some(expectedBody.noSpaces), Some(`Content-Type`(application.json)))

  def check(responseIO: IO[Response[IO]], expectedStatus: Status, expectedBody: String): IO[Unit] =
    check(responseIO, expectedStatus, Some(expectedBody), Some(`Content-Type`(text.plain, Charset.`UTF-8`)))

  def check(
    io: IO[Response[IO]],
    expectedStatus: Status,
    expectedBody: Option[String] = None,
    expectedContentType: Option[`Content-Type`] = None,
    evaluateBody: Boolean = true
  ): IO[Unit] = io.flatMap { response =>
    assertEquals(response.status, expectedStatus)
    if (evaluateBody) {
      assertEquals(response.headers.get(`Content-Type`), expectedContentType)
      response.as[String].assertEquals(expectedBody.getOrElse(""))
    } else {
      IO.unit
    }
  }
}
