val circeVersion      = "0.13.0"
val pureconfigVersion = "0.14.0"
val http4sVersion     = "0.21.7"
val tapirVersion      = "0.16.16"
val odinVersion       = "0.8.1"

ThisBuild / scalafixDependencies += "com.github.liancheng" %% "organize-imports" % "0.4.0"

lazy val graalnative4s = project
  .in(file("."))
  .enablePlugins(BuildInfoPlugin, sbtdocker.DockerPlugin, GraalVMNativeImagePlugin)
  .settings(
    scalaVersion := "2.13.3",
    organization := "dev.sommerlatt",
    libraryDependencies ++= Seq(
      compilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1"),
      "com.github.pureconfig"       %% "pureconfig"               % pureconfigVersion,
      "com.github.pureconfig"       %% "pureconfig-cats-effect"   % pureconfigVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-core"               % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-http4s-server"      % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-json-circe"         % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-openapi-docs"       % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-openapi-circe-yaml" % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-http4s"  % tapirVersion,
      "com.github.valskalla"        %% "odin-core"                % odinVersion,
      "com.github.valskalla"        %% "odin-slf4j"               % odinVersion,
      "io.circe"                    %% "circe-core"               % circeVersion,
      "io.circe"                    %% "circe-generic"            % circeVersion,
      "io.circe"                    %% "circe-parser"             % circeVersion,
      "io.circe"                    %% "circe-literal"            % circeVersion,
      "io.circe"                    %% "circe-generic-extras"     % circeVersion,
      "org.http4s"                  %% "http4s-blaze-server"      % http4sVersion,
      "org.http4s"                  %% "http4s-circe"             % http4sVersion,
      "org.http4s"                  %% "http4s-dsl"               % http4sVersion
    ),
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
    buildInfoPackage := organization.value,
    buildInfoOptions ++= Seq[BuildInfoOption](BuildInfoOption.ToMap, BuildInfoOption.BuildTime),
    semanticdbEnabled := true,
    semanticdbVersion := scalafixSemanticdb.revision,
    dockerfile in docker := NativeDockerfile(file("Dockerfile")),
    imageNames in docker := Seq(ImageName(s"usommerl/${name.value}:${versionTag.getOrElse("latest")}"))
  )

def versionTag: Option[String] = {
  import sys.process._
  val regex = """v\d+\.\d+\.\d+""".r.regex
  val versionTags =
    "git tag --points-at HEAD".!!.trim.split("\n").filter(_.matches(regex))
  versionTags.sorted(Ordering.String.reverse).headOption.map(_.replace("v", ""))
}
