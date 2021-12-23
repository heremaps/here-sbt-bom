import Dependencies._
import com.here.bom.Bom

lazy val deps = Bom.read("com.fasterxml.jackson" % "jackson-bom" % "2.14.2")(bom => Dependencies(bom))

lazy val `demo` = project
  .in(file("."))
  .settings(scalaVersion := "2.12.15")
  .settings(deps)
  .settings(
    name := "simple-test",
    libraryDependencies ++= deps.key.value.dependencies,
    resolvers := Resolver.DefaultMavenRepository +: resolvers.value,
  )
