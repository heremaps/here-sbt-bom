import com.here.bom.Bom
import scala.sys.process.Process

lazy val scalaCompilerOptions = Seq(
  "-deprecation",
  "-encoding",
  "UTF-8",
  "-feature",
  "-language:existentials",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-unchecked",
  "-Xfatal-warnings",
  "-Xlint",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
  "-Ywarn-value-discard",
)

lazy val akkaBom = Bom("com.lightbend.akka" %% "akka-dependencies" % "23.05.4")
def akkaDependencies(bom: Bom) = {
  Seq(
    "com.lightbend.akka.grpc" %% "akka-grpc-runtime" % bom
  )
}

lazy val `test-downloaded-deps` = project
  .in(file("."))
  .settings(akkaBom)
  .settings(
    name := "test-downloaded-deps",
    scalaVersion := "2.13.2",
    resolvers := Resolver.DefaultMavenRepository +: resolvers.value,
    libraryDependencies ++= akkaDependencies(akkaBom.key.value),
    dependencyOverrides ++= akkaBom.key.value.bomDependencies,
    TaskKey[Unit]("akkaDepsDescriptorExist") := {
      val akkaDepsPath =f"${System.getProperty("java.io.tmpdir")}/test-sbt-bom-cache/cache/com.lightbend.akka/akka-dependencies_2.13/ivy-23.05.4.xml"
      if (!new File(akkaDepsPath).exists()) {
        sys.error(f"Akka deps file is not exists: $akkaDepsPath")
      }
    },
    TaskKey[Unit]("akkaGrpcRuntimeDescriptorNotExist") := {
      val akkaDepsPath =f"${System.getProperty("java.io.tmpdir")}/test-sbt-bom-cache/cache/com.lightbend.akka.grpc/akka-grpc-runtime_2.13"
      if (new File(akkaDepsPath).exists()) {
        sys.error(f"GRPC core exits: $akkaDepsPath")
      }
    }
  )

ThisBuild / scalacOptions ++= scalaCompilerOptions
Global / onChangedBuildSource := ReloadOnSourceChanges
