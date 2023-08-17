import sbtrelease.ReleasePlugin.autoImport.ReleaseTransformations._
import sbtrelease.Version
import scala.xml._

val organizationSettings: Seq[Setting[_]] = Seq(
  scalaVersion := "2.12.15",
  organization := "com.here.platform",
  projectInfo := ModuleInfo(
    nameFormal = "HERE sbt-bom plugin",
    description =
      "The sbt-bom plugin sbt-bom plugin provides a way to use Maven BOM (bill of materials) in sbt projects",
    homepage = Some(url("http://here.com")),
    startYear = Some(2021),
    licenses = Vector(),
    organizationName = "HERE Europe B.V",
    organizationHomepage = Some(url("http://here.com")),
    scmInfo = Some(
      ScmInfo(
        connection = "scm:git:https://github.com/heremaps/here-sbt-bom.git",
        devConnection = "scm:git:git@github.com:heremaps/here-sbt-bom.git",
        browseUrl = url("https://github.com/heremaps/here-sbt-bom")
      )
    ),
    developers = Vector(
      Developer(
        "here",
        "HERE Artifact Service Team",
        "ARTIFACT_SERVICE_SUPPORT@here.com",
        url = url("https://github.com/heremaps")
      )
    )
  )
)

lazy val licenseSettings = Seq(
  headerEmptyLine := false,
  headerLicense := Some(HeaderLicense.Custom(IO.read(file("./LICENSE")))),
  Compile / headerSources ++= {
    val base = baseDirectory.value
    val baseDirectories = (base / "src" / "sbt-test" / "bom")
    val customJars = (baseDirectories ** "*.scala")
    customJars.get()
  }
)

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
  "-Yno-adapted-args",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
  "-Ywarn-value-discard",
  "-Xfuture"
)

lazy val commonSettings = organizationSettings ++ licenseSettings

lazy val `root` = project
  .in(file("."))
  .settings(
    publish := {
      print("Skip publishing root artifacts")
    }
  )
  .settings(commonSettings)
  .aggregate(`sbt-bom`)

lazy val `sbt-bom` = project
  .in(file("plugin"))
  .enablePlugins(SbtPlugin)
  .settings(publishSettings)
  .settings(commonSettings)
  .settings(
    Compile / unmanagedResources ++= Seq(baseDirectory.value / "LICENSE"),
    // Testing
    scriptedLaunchOpts := {
      scriptedLaunchOpts.value ++
        Seq("-Dplugin.version=" + version.value)
    },
    scriptedBufferLog := false,
    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest" % "3.2.15" % Test,
      "org.scalamock" %% "scalamock" % "5.2.0" % Test
    )
  )

lazy val publishSettings = Seq(
  ThisBuild / publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases" at nexus + "service/local/staging/deploy/maven2")
  },
  pomIncludeRepository := { _ => false },
  publishMavenStyle := true,
  publishConfiguration := publishConfiguration.value.withOverwrite(true),
  pomPostProcess := {
    //    replace artifactId from `sbt-bom` to `sbt-bom_<scala_binary_version>_<sbt_binary_version>` in the generated pom.xml file https://github.com/sbt/sbt/issues/3410
    case elem: Elem =>
      val updatedChildNodes = elem.child.map {
        case childElem: Elem if childElem.label == "artifactId" =>
          childElem.copy(child = Text("sbt-bom_2.12_1.0"))
        case other => other
      }
      elem.copy(child = updatedChildNodes)
    case other => other
  }
)

pomExtra :=
  <licenses>
    <license>
      <name>Apache License, Version 2.0</name>
      <url>https://www.apache.org/licenses/LICENSE-2.0.txt</url>
      <distribution>repo</distribution>
    </license>
  </licenses>

ThisBuild / scalacOptions ++= scalaCompilerOptions
Global / onChangedBuildSource := ReloadOnSourceChanges

useGpgAgent := false
useGpgPinentry := true
sonatypeProfileName := "com.here"

// Defines the release process
releaseIgnoreUntrackedFiles := true

releaseTagName := (ThisBuild / version).value
releaseTagComment := s"Release ${(ThisBuild / version).value} from build ${sys.env
    .getOrElse("TRAVIS_BUILD_ID", "None")}"
releaseNextVersion := { ver =>
  Version(sys.props.getOrElse("currentVersion", ver))
    .map(_.bump(releaseVersionBump.value).string)
    .getOrElse(sbtrelease.versionFormatError(ver))
}

commands += Command.command("prepareRelease")((state: State) => {
  println("Preparing release...")
  val projectState = Project extract state
  val customState = projectState.appendWithoutSession(
    Seq(
      releaseProcess := Seq[ReleaseStep](
        checkSnapshotDependencies,
        inquireVersions,
        setNextVersion,
        runClean,
        runTest,
        tagRelease
      )
    ),
    state
  )
  Command.process("release with-defaults", customState)
})
