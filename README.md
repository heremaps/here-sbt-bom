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
- Scala 2.12
- sbt 1.3.0 or newer

## How to use it
The following text provides a step-by-step guide on how to import [Jackson BOM](https://github.com/FasterXML/jackson-bom) into an sbt project.

Enable the `MAVEN_CENTRAL` repository with a special `pattern` configuration:
```
resolvers += Resolver.url(
  "MAVEN_CENTRAL",
  url("https://repo.maven.apache.org/maven2"))(
  Patterns("[organisation]/[module]/[revision]/[artifact]-[revision](-[classifier]).[ext]") )
```
This will enable the project to access plugin files stored in Maven Central, https://repo.maven.apache.org/maven2/com/here/platform/sbt-bom_2.12_1.0/.

Add the `sbt-bom` plugin to `plugins.sbt` file.
```
addSbtPlugin("com.here.platform" % "sbt-bom" % "1.0.1")
```

Add BOM dependency to the `build.sbt`:
```
import com.here.bom.Bom
lazy val jacksonBom = Bom.read("com.fasterxml.jackson" % "jackson-bom" % "2.14.2")(bom => Dependencies(bom))
```

This creates an sbt `Setting` that resolves a desired BOM, creates a `Bom` object from it and then builds a `Dependencies` object.
Under the `project` directory, create a `Dependencies` class that takes a `Bom` as a constructor parameter.
Inside this class, we can refer to the versions from the BOM.
The class should expose a list of dependencies that used as `libraryDependencies`
```
import sbt._
import com.here.bom.Bom

case class Dependencies(bom: Bom) {
  val dependencies: Seq[ModuleID] = Seq(
    "com.fasterxml.jackson.core" % "jackson-databind" % bom
  )
}
```

Enable this setting in the module configuration and use this setting in `libraryDependencies`:
```
lazy val `demo` = project
  .in(file("."))
  .settings(jacksonBom)
  .settings(
    libraryDependencies ++= jacksonBom.key.value.dependencies
  )
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
The user name and password can be retrieved as described in the [Get your repository credentials tutorial](https://developer.here.com/documentation/java-scala-dev/dev_guide/topics/get-credentials.html#get-your-repository-credentials).

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
Now the project is configured to use BOM files from a password-protected repository. For example, [`sdk-batch-bom`](https://developer.here.com/documentation/java-scala-dev/dev_guide/sdk-libraries.html#sdk-libraries-for-batch-primary) can be used.
```sbt
lazy val sdkBom = Bom.read("com.here.platform" %% "sdk-batch-bom" % "2.52.8")(bom => Dependencies(bom))
```

The credentials can be supplied through these methods, with the following precedence:
1. By passing the command line parameter `-Dsbt.boot.credentials=<file>`.
2. Via the environment variable `SBT_CREDENTIALS=<file>`.
3. By creating the file `~/.sbt/.credentials` in your home directory.
4. By creating the file `~/.ivy2/.credentials` in your home directory.

## Contributors
- Evgenii Kuznetcov (https://github.com/simpadjo)
- Oleksandr Vyshniak (https://github.com/molekyla)
- Andrii Banias (https://github.com/abanias)
- Anton Wilhelm (https://github.com/devtonhere)
- Dmitry Abramov (https://github.com/dmitriy-abramov)

## License
Copyright (C) 2019-2023 HERE Europe B.V.

Unless otherwise noted in `LICENSE` files for specific files or directories, the [LICENSE](LICENSE) in the root applies to all content in this repository.
