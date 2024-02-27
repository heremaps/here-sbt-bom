import com.here.bom.Bom

lazy val slf4jDeps = Bom.read("org.slf4j" % "slf4j-bom" % "2.0.12")(bom => "org.slf4j" % "slf4j-log4j12" % bom)
lazy val root = (project in file("."))
  .settings(slf4jDeps)
  .settings(libraryDependencies += slf4jDeps.key.value)
