import Dependencies._
import com.here.bom.Bom

ThisBuild / libraryDependencySchemes +=
  "org.scala-lang.modules" %% "scala-xml" % "always"

lazy val deps =
  Bom.read("com.here.platform" %% "sdk-stream-bom" % "2.49.2")(
    bom => Dependencies(bom)
  )

lazy val demo = project
  .in(file("."))
  .settings(scalaVersion := "2.12.20")
  .settings(deps)
  .settings(
    name := "simple-test",
    libraryDependencies ++= deps.key.value.dependencies,
    credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")
  )

ThisBuild / resolvers ++= additionalResolvers