ThisBuild / scalaVersion := "3.5.0"
ThisBuild / organization := "dev.usommerl"

ThisBuild / libraryDependencySchemes += "com.softwaremill.sttp.apispec" %% "openapi-model" % "early-semver"
ThisBuild / libraryDependencySchemes += "com.softwaremill.sttp.apispec" %% "apispec-model" % "early-semver"

val v = new {
  val apispec = "0.11.3"
  val circe   = "0.14.10"
  val ciris   = "3.6.0"
  val http4s  = "0.23.28"
  val odin    = "0.14.0"
  val tapir   = "1.11.3"
  val munit   = "1.0.2"
  val munitCE = "2.0.0"
}

val upx = "UPX_COMPRESSION"

lazy val graalnative4s = project
  .in(file("."))
  .enablePlugins(BuildInfoPlugin, sbtdocker.DockerPlugin, GraalVMNativeImagePlugin)
  .settings(
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.apispec" %% "openapi-circe-yaml"  % v.apispec,
      "com.softwaremill.sttp.tapir"   %% "tapir-core"          % v.tapir,
      "com.softwaremill.sttp.tapir"   %% "tapir-http4s-server" % v.tapir,
      "com.softwaremill.sttp.tapir"   %% "tapir-json-circe"    % v.tapir,
      "com.softwaremill.sttp.tapir"   %% "tapir-openapi-docs"  % v.tapir,
      "com.softwaremill.sttp.tapir"   %% "tapir-refined"       % v.tapir,
      "com.softwaremill.sttp.tapir"   %% "tapir-swagger-ui"    % v.tapir,
      "dev.scalafreaks"               %% "odin-core"           % v.odin,
      "dev.scalafreaks"               %% "odin-json"           % v.odin,
      "dev.scalafreaks"               %% "odin-slf4j"          % v.odin,
      "io.circe"                      %% "circe-core"          % v.circe,
      "io.circe"                      %% "circe-generic"       % v.circe,
      "io.circe"                      %% "circe-parser"        % v.circe,
      "io.circe"                      %% "circe-literal"       % v.circe,
      "is.cir"                        %% "ciris"               % v.ciris,
      "is.cir"                        %% "ciris-refined"       % v.ciris,
      "org.http4s"                    %% "http4s-ember-server" % v.http4s,
      "org.http4s"                    %% "http4s-circe"        % v.http4s,
      "org.http4s"                    %% "http4s-dsl"          % v.http4s,
      "org.scalameta"                 %% "munit"               % v.munit   % Test,
      "org.typelevel"                 %% "munit-cats-effect"   % v.munitCE % Test
    ),
    testFrameworks += new TestFramework("munit.Framework"),
    buildInfoKeys                    := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion, Test / libraryDependencies),
    buildInfoPackage                 := organization.value,
    buildInfoOptions ++= Seq[BuildInfoOption](BuildInfoOption.BuildTime),
    semanticdbEnabled                := true,
    semanticdbVersion                := scalafixSemanticdb.revision,
    docker / dockerfile              := NativeDockerfile(file("Dockerfile")),
    docker / imageNames              := Seq(ImageName(s"ghcr.io/usommerl/${name.value}:$dockerImageTag")),
    docker / dockerBuildArguments    := dockerBuildArgs,
    assembly / test                  := (Test / test).value,
    assembly / assemblyMergeStrategy := {
      case "META-INF/maven/org.webjars/swagger-ui/pom.properties" => MergeStrategy.singleOrError
      case x if x.endsWith("module-info.class")                   => MergeStrategy.discard
      case x                                                      => (assembly / assemblyMergeStrategy).value(x)
    }
  )

def dockerImageTag: String = {
  import sys.process._
  val regex       = """v\d+\.\d+\.\d+""".r.regex
  val versionTags = "git tag --points-at HEAD".!!.trim.split("\n").filter(_.matches(regex))
  val version     = versionTags.sorted(Ordering.String.reverse).headOption.map(_.replace("v", "")).getOrElse("latest")
  val upxSuffix   = sys.env.get(upx).map(s => s"-upx${s.replace("--", "-")}").getOrElse("")
  s"$version$upxSuffix"
}

def dockerBuildArgs: Map[String, String] =
  sys.env.foldLeft(Map.empty[String, String]) { case (acc, (k, v)) =>
    if (Set("UPX_COMPRESSION", "PRINT_REPORTS").contains(k)) acc + (k.toLowerCase -> v) else acc
  }
