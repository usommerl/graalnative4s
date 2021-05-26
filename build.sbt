ThisBuild / scalaVersion := "2.13.6"
ThisBuild / organization := "dev.usommerl"
ThisBuild / scalafixDependencies += "com.github.liancheng" %% "organize-imports" % "0.5.0"

val v = new {
  val http4s  = "0.21.23"
  val circe   = "0.14.0"
  val ciris   = "1.2.1"
  val tapir   = "0.17.19"
  val odin    = "0.11.0"
  val munit   = "0.7.26"
  val munitCE = "1.0.3"
}

val upx = "UPX_COMPRESSION"

lazy val graalnative4s = project
  .in(file("."))
  .enablePlugins(BuildInfoPlugin, sbtdocker.DockerPlugin, GraalVMNativeImagePlugin)
  .settings(
    libraryDependencies ++= Seq(
      compilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1"),
      "com.softwaremill.sttp.tapir" %% "tapir-core"               % v.tapir,
      "com.softwaremill.sttp.tapir" %% "tapir-http4s-server"      % v.tapir,
      "com.softwaremill.sttp.tapir" %% "tapir-json-circe"         % v.tapir,
      "com.softwaremill.sttp.tapir" %% "tapir-openapi-docs"       % v.tapir,
      "com.softwaremill.sttp.tapir" %% "tapir-openapi-circe-yaml" % v.tapir,
      "com.softwaremill.sttp.tapir" %% "tapir-refined"            % v.tapir,
      "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-http4s"  % v.tapir,
      "com.github.valskalla"        %% "odin-core"                % v.odin,
      "com.github.valskalla"        %% "odin-json"                % v.odin,
      "com.github.valskalla"        %% "odin-slf4j"               % v.odin,
      "io.circe"                    %% "circe-core"               % v.circe,
      "io.circe"                    %% "circe-generic"            % v.circe,
      "io.circe"                    %% "circe-parser"             % v.circe,
      "io.circe"                    %% "circe-literal"            % v.circe,
      "is.cir"                      %% "ciris"                    % v.ciris,
      "is.cir"                      %% "ciris-refined"            % v.ciris,
      "org.http4s"                  %% "http4s-blaze-server"      % v.http4s,
      "org.http4s"                  %% "http4s-circe"             % v.http4s,
      "org.http4s"                  %% "http4s-dsl"               % v.http4s,
      "org.scalameta"               %% "munit"                    % v.munit   % Test,
      "org.typelevel"               %% "munit-cats-effect-2"      % v.munitCE % Test
    ),
    testFrameworks += new TestFramework("munit.Framework"),
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion, Test / libraryDependencies),
    buildInfoPackage := organization.value,
    buildInfoOptions ++= Seq[BuildInfoOption](BuildInfoOption.BuildTime),
    semanticdbEnabled := true,
    semanticdbVersion := scalafixSemanticdb.revision,
    docker / dockerfile := NativeDockerfile(file("Dockerfile")),
    docker / imageNames := Seq(ImageName(s"ghcr.io/usommerl/${name.value}:${dockerImageTag}")),
    docker / dockerBuildArguments := sys.env.get(upx).map(s => Map("upx_compression" -> s)).getOrElse(Map.empty),
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
