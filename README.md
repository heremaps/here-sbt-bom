[![Build Status](https://github.com/heremaps/here-sbt-bom/actions/workflows/release.yml/badge.svg)](https://github.com/heremaps/here-sbt-bom/actions?query=workflow%3ARelease+branch%3Amaster)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.here.platform/root_2.12/badge.svg)](https://search.maven.org/artifact/com.here.platform/sbt-bom_2.12_1.0)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

# HERE SBT BOM plugin

## Introduction
The HERE platform SBT BOM plugin provides a way to use Maven [BOM (bill of materials)](https://maven.apache.org/guides/introduction/introduction-to-dependency-mechanism.html#bill-of-materials-bom-poms) in SBT projects.
The plugin features importing multiple BOM files to use as a source of dependency versions, automatically processing parent and dependent BOMs.

## Limitation
The HERE platform SBT BOM plugin is provided "as is" and is not officially a part of HERE Workspace or HERE Marketplace.
While there is no official support by HERE, you may still raise issues via GitHub. We may be able to help.

## Prerequisites
This plugin is compatible with:
- sbt 1.3.0 or newer

## How to use it
The following text provides a step-by-step guide on how to import [Jackson BOM](https://github.com/FasterXML/jackson-bom) into an sbt project.

### Load the plugin

Add the `sbt-bom` plugin to `plugins.sbt` file.
```scala
addSbtPlugin("com.here.platform" % "sbt-bom" % "1.0.14")
```

#### Add Maven central repository in your resolvers

If you're encountering issues or using earlier versions, you might have to explicitly declare the Maven Central repository:
```scala
resolvers += Resolver.url(
  "MAVEN_CENTRAL",
  url("https://repo.maven.apache.org/maven2"))(
  Patterns("[organisation]/[module]/[revision]/[artifact]-[revision](-[classifier]).[ext]") )
```
This will enable the project to access plugin files stored in Maven Central, https://repo.maven.apache.org/maven2/com/here/platform/sbt-bom_2.12_1.0/.

### Read & use a BOM

There are different ways to use the plugin depending on your use case.

#### Use it in `dependencyOverrides`  (`Bom.dependencies`)

If you want to get all the dependencies declared in the BOM and use their version to override yours.

Use the following setup in your `build.sbt`:
```scala
import com.here.bom.Bom

// Declare the BOM
// Note the usage of Bom.dependencies method
lazy val jacksonDependencies = Bom.dependencies("com.fasterxml.jackson" % "jackson-bom" % "2.14.2")

lazy val `demo` = project
  .in(file("."))
  // Load the BOM
  .settings(jacksonDependencies)
  // Add all dependencies of the BOM in dependencyOverrides
  .settings(
    dependencyOverrides ++= jacksonDependencies.key.value
  )
```

#### Use it in `dependencyOverrides` and `libraryDependencies` (`Bom.apply`)

If you want to:
- get all the dependencies declared in the BOM and use their version to override yours,
- also use the version of a BOM dependency explicitly

Use the following setup in your `build.sbt`:
```scala
import com.here.bom.Bom

// Declare the BOM
// Note the usage of Bom.apply method
lazy val jacksonBom = Bom("com.fasterxml.jackson" % "jackson-bom" % "2.14.2")

lazy val `demo` = project
  .in(file("."))
  // Load the BOM
  .settings(jacksonBom)
  // Add some explicit dependencies
  .settings(
    libraryDependencies += "com.fasterxml.jackson.core" % "jackson-core" % jacksonBom.key.value
  )
  // Add all dependencies of the BOM in dependencyOverrides
  .settings(
    dependencyOverrides ++= jacksonBom.key.value.bomDependencies
  )
```

#### Custom usage (`Bom.read`)

In addition to the common usages described above, you can also use the BOM in a custom way thanks to `Bom.read`.

Example, in your `build.sbt`:
```scala
import com.here.bom.Bom

// Declare the BOM and map it to a custom class
// Note the usage of Bom.read method
lazy val jacksonDependencies = Bom.read("com.fasterxml.jackson" % "jackson-bom" % "2.14.2")(bom => Dependencies(bom))

lazy val `demo` = project
  .in(file("."))
  // Load the BOM
  .settings(jacksonDependencies)
  // Add some explicit dependencies
  .settings(
    libraryDependencies ++= jacksonDependencies.key.value.someDependencies
  )
  // Add all dependencies of the BOM in dependencyOverrides
  .settings(
    dependencyOverrides ++= jacksonDependencies.key.value.allDependencies
  )
```

And in a `project/Dependencies.scala` file:
```scala
import sbt._
import com.here.bom.Bom

case class Dependencies(bom: Bom) {
  val someDependencies: Seq[ModuleID] = Seq(
    "com.fasterxml.jackson.core" % "jackson-core" % bom
  )
  
  val allDependencies: Seq[ModuleID] = bom.bomDependencies
}
```

### Password-protected repositories
The following text explains how to access password-protected repositories, specifically the HERE platform repository,
using a file with credentials.

For accessing password-protected repositories, such as the HERE platform repository, you need to create a file with credentials at `~/.sbt/.credentials`
with the following content:
```
realm=Artifactory Realm
host=repo.platform.here.com
user=<your used id>
password=<top secret password>
```
The user name and password can be retrieved as described in the [Get your repository credentials tutorial](https://www.here.com/docs/bundle/here-workspace-developer-guide-java-scala/page/topics/get-credentials.html#get-your-repository-credentials).

Then include the credentials in `build.sbt` file:
```sbt
project.settings(
  credentials += Credentials(Path.userHome / ".sbt" / ".credentials")
)
```
and include the HERE platform repository resolver:
```sbt
ThisBuild / resolvers += (
  "HERE Platform Repository" at "https://repo.platform.here.com/artifactory/open-location-platform"
)
```
Now the project is configured to use BOM files from a password-protected repository. For example, [`sdk-batch-bom`](https://www.here.com/docs/bundle/here-workspace-developer-guide-java-scala/page/sdk-libraries.html#sdk-libraries-for-batch-primary) can be used.
```sbt
lazy val sdkBom = Bom.read("com.here.platform" %% "sdk-batch-bom" % "2.57.3")(bom => Dependencies(bom))
```

The credentials can be supplied through these methods, with the following precedence:
1. By passing the command line parameter `-Dsbt.boot.credentials=<file>`.
2. Via the environment variable `SBT_CREDENTIALS=<file>`.
3. By creating the file `~/.sbt/.credentials` in your home directory.
4. By creating the file `~/.ivy2/.credentials` in your home directory.

### Resolving issues
If your project packaging fails, you can examine the plugin debug logs using the following commands:
```shell
sbt package
sbt last
```
This sequence of commands will print all the logs generated during packaging to the console.

## Contributors
- Evgenii Kuznetcov (https://github.com/simpadjo)
- Oleksandr Vyshniak (https://github.com/molekyla)
- Andrii Banias (https://github.com/abanias)
- Anton Wilhelm (https://github.com/devtonhere)
- Dmitry Abramov (https://github.com/dmitriy-abramov)
- Gaël Jourdan-Weil (https://github.com/gaeljw)

## License
Copyright (C) 2019-2024 HERE Europe B.V.

Unless otherwise noted in `LICENSE` files for specific files or directories, the [LICENSE](LICENSE) in the root applies to all content in this repository.
