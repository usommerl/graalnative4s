ThisBuild / scalaVersion := "2.13.3"
ThisBuild / organization := "dev.usommerl"
ThisBuild / scalafixDependencies += "com.github.liancheng" %% "organize-imports" % "0.4.4"

val v = new {
  val http4s     = "0.21.9"
  val circe      = "0.13.0"
  val tapir      = "0.16.16"
  val odin       = "0.9.1"
  val pureconfig = "0.14.0"
  val munit      = "0.7.18"
  val munitCE    = "0.9.0"
}

lazy val graalnative4s = project
  .in(file("."))
  .enablePlugins(BuildInfoPlugin, sbtdocker.DockerPlugin, GraalVMNativeImagePlugin)
  .settings(
    libraryDependencies ++= Seq(
      compilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1"),
      "com.github.pureconfig"       %% "pureconfig"               % v.pureconfig,
      "com.github.pureconfig"       %% "pureconfig-cats-effect"   % v.pureconfig,
      "com.softwaremill.sttp.tapir" %% "tapir-core"               % v.tapir,
      "com.softwaremill.sttp.tapir" %% "tapir-http4s-server"      % v.tapir,
      "com.softwaremill.sttp.tapir" %% "tapir-json-circe"         % v.tapir,
      "com.softwaremill.sttp.tapir" %% "tapir-openapi-docs"       % v.tapir,
      "com.softwaremill.sttp.tapir" %% "tapir-openapi-circe-yaml" % v.tapir,
      "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-http4s"  % v.tapir,
      "com.github.valskalla"        %% "odin-core"                % v.odin,
      "com.github.valskalla"        %% "odin-slf4j"               % v.odin,
      "io.circe"                    %% "circe-core"               % v.circe,
      "io.circe"                    %% "circe-generic"            % v.circe,
      "io.circe"                    %% "circe-parser"             % v.circe,
      "io.circe"                    %% "circe-literal"            % v.circe,
      "io.circe"                    %% "circe-generic-extras"     % v.circe,
      "org.http4s"                  %% "http4s-blaze-server"      % v.http4s,
      "org.http4s"                  %% "http4s-circe"             % v.http4s,
      "org.http4s"                  %% "http4s-dsl"               % v.http4s,
      "org.scalameta"               %% "munit"                    % v.munit   % Test,
      "org.typelevel"               %% "munit-cats-effect-2"      % v.munitCE % Test
    ),
    testFrameworks += new TestFramework("munit.Framework"),
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion) ++ Seq[BuildInfoKey](Test / libraryDependencies),
    buildInfoPackage := organization.value,
    buildInfoOptions ++= Seq[BuildInfoOption](BuildInfoOption.BuildTime),
    semanticdbEnabled := true,
    semanticdbVersion := scalafixSemanticdb.revision,
    docker / dockerfile := NativeDockerfile(file("Dockerfile")),
    docker / imageNames := Seq(ImageName(s"ghcr.io/usommerl/${name.value}:${dockerImageTag}"))
  )

def dockerImageTag: String = {
  import sys.process._
  val regex       = """v\d+\.\d+\.\d+""".r.regex
  val versionTags = "git tag --points-at HEAD".!!.trim.split("\n").filter(_.matches(regex))
  versionTags.sorted(Ordering.String.reverse).headOption.map(_.replace("v", "")).getOrElse("latest")
}
