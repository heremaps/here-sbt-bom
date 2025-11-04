/*
 * Copyright (C) 2019-2025 HERE Europe B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * License-Filename: LICENSE
 */
package com.here.bom.internal

import lmcoursier.internal.shaded.coursier.core.shaded.geny.Generator.from
import sbt.File
import sbt.util.Logger

import java.net.{URI, URL}
import java.io.FileInputStream
import java.util.Properties
import scala.collection.JavaConverters.*

object IvyPomLocator {
  /*
     Ivy internals require ivy.home and sbt.ivy.home to be equals and have absolute path.
     Related sbt issue: https://github.com/sbt/sbt/issues/1894
     Example of error if this doesn't hold:

     java.lang.IllegalArgumentException: ivy.home must be absolute: .sbt-cache/ivy2
            at org.apache.ivy.util.Checks.checkAbsolute(Checks.java:57)
            at org.apache.ivy.core.settings.IvySettings.getDefaultIvyUserDir(IvySettings.java:801)
            at org.apache.ivy.core.settings.IvySettings.getDefaultCache(IvySettings.java:824)
            at org.apache.ivy.core.settings.IvySettings.getDefaultResolutionCacheBasedir(IvySettings.java:864)
            at sbt.internal.librarymanagement.IvySbt$.$anonfun$configureResolutionCache$1(Ivy.scala:582)

   */
  def tweakIvyHome(logger: Logger): Unit = {
    var sbtIvyHome = System.getProperty("sbt.ivy.home")
    var ivyHome = System.getProperty("ivy.home")
    if (sbtIvyHome != null && ivyHome != null) {
      sbtIvyHome = convertToAbsolutePath(sbtIvyHome)
      ivyHome = convertToAbsolutePath(ivyHome)
      if (sbtIvyHome != ivyHome) {
        logger.error(
          s"System properties ivy.home and sbt.ivy.home have different values: $ivyHome and $sbtIvyHome. Consider use the same value"
        )
        throw new RuntimeException(
          "System properties ivy.home and sbt.ivy.home must have same value"
        )
      }
      adjustSystemProperty(logger, "sbt.ivy.home", sbtIvyHome)
      adjustSystemProperty(logger, "ivy.home", ivyHome)
    } else if (sbtIvyHome != null) {
      sbtIvyHome = convertToAbsolutePath(sbtIvyHome)
      adjustSystemProperty(logger, "sbt.ivy.home", sbtIvyHome)
      adjustSystemProperty(logger, "ivy.home", sbtIvyHome)
    } else if (ivyHome != null) {
      ivyHome = convertToAbsolutePath(ivyHome)
      adjustSystemProperty(logger, "sbt.ivy.home", ivyHome)
      adjustSystemProperty(logger, "ivy.home", ivyHome)
    }
  }

  private def adjustSystemProperty(
      logger: Logger,
      systemPropertyName: String,
      value: String
  ): Unit = {
    val oldValue = System.setProperty(systemPropertyName, value)
    if (oldValue != value) {
      logger.warn(s"Adjusting $systemPropertyName: $oldValue -> $value")
    }
  }

  private def convertToAbsolutePath(path: String): String = {
    new File(path).getAbsolutePath
  }
}

/**
 * Class responsible for searching a pom file of a dependency
 */
class IvyPomLocator(resolver: DependencyResolutionProxy, ivyHome: File, logger: Logger) {

  def getPomFile(moduleId: NormalizedArtifact): Option[File] = {
    findLocalPomFile(moduleId).orElse {
      val dummyDir = new File("target/boms")
      logger.info(s"Resolving ${moduleId}")
      resolver.retrieve(
        resolver.wrapDependencyInModule(moduleId.asModule().intransitive()),
        dummyDir,
        logger
      ) match {
        case Left(warnings) =>
          logger.warn(warnings.resolveException.messages.size.toString)
          warnings.resolveException.messages.foreach(logger.warn(_))
          logger.trace(warnings.resolveException)
        case Right(_) => ()
      }
      findLocalPomFile(moduleId)
    }
  }

  private def findLocalPomFile(moduleId: NormalizedArtifact): Option[File] =
    resolvePomUrl(moduleId).flatMap(urlToLocalFile)

  def getPomUrl(moduleId: NormalizedArtifact): Option[URL] =
    resolvePomUrl(moduleId)

  /** Canonical local ivy original path (if Ivy materialized it). */
  private def ivyOriginalPath(moduleId: NormalizedArtifact): File =
    new File(ivyHome, s"/cache/${moduleId.group}/${moduleId.name}/ivy-${moduleId.version}.xml.original")

  /** ivydata-<ver>.properties path. */
  private def ivyDataPath(moduleId: NormalizedArtifact): File =
    new File(ivyHome, s"/cache/${moduleId.group}/${moduleId.name}/ivydata-${moduleId.version}.properties")

  /** Convert URL → File only for file: scheme. */
  private def urlToLocalFile(u: URL): Option[File] =
    if (u.getProtocol.equalsIgnoreCase("file")) Some(new File(u.toURI)) else None

  /**
   * If the artifact was resolved by Ivy, the POM will be under ~/.ivy2/cache/... as
   * ivy-<ver>.xml.original. If it was previously resolved by Maven, Ivy may not download it again
   * and only store a reference in ivydata-<ver>.properties. That .location can be a local
   * filesystem path (~/.m2/...) or a remote URL (https://...).
   *
   * Examples: 1) Local POM (ivy):
   * ~/.ivy2/cache/com.here.platform/sdk-dep-common_2.12/ivydata-1.0.46.properties
   * artifact:...pom.original...location=~/.m2/repository/.../sdk-dep-common_2.12-1.0.46.pom ...
   * is-local=true
   *
   * 2) Remote POM (downloaded by Maven first):
   * ~/.ivy2/cache/com.here.platform/sdk-dep-common_2.12/ivydata-1.0.47.properties
   * artifact:...pom.original...location=https://artifactory.../sdk-dep-common_2.12-1.0.47.pom ...
   * is-local=false
   *
   * Resolution strategy:
   *   - If ivy-<ver>.xml.original exists => return its file: URL.
   *   - Else read ivydata .location: * if it parses as URL with scheme => return that URL (handles
   *     https:// or file:/) * else if it's a filesystem path that exists => return file: URL * else
   *     \=> None
   */

  private def resolvePomUrl(moduleId: NormalizedArtifact): Option[URL] = {
    val local = ivyOriginalPath(moduleId)
    if (local.exists()) return Some(local.toURI.toURL)
    referencedPomUrl(ivyDataPath(moduleId))
  }

  /** Read a usable URL from ivydata (normalizes “https\://”, handles file paths). */
  private def referencedPomUrl(ivyPropertyFile: File): Option[URL] = {
    if (!ivyPropertyFile.exists()) return None

    val properties = new Properties()
    val is = new FileInputStream(ivyPropertyFile)
    try properties.load(is)
    finally is.close()

    val locationKeyOpt: Option[String] =
      properties
        .keys()
        .asScala
        .collect { case s: String if s.endsWith(".location") => s }
        .headOption

    locationKeyOpt.flatMap { locationKey =>
      val rawLocation0 = properties.getProperty(locationKey)
      val rawLocation = Option(rawLocation0).map(_.replace("\\:", ":"))

      rawLocation.flatMap { loc =>
        // Prefer interpreting as a URI; if it has a scheme, we’re done.
        try {
          val uri = new URI(loc)
          if (uri.getScheme != null) Some(uri.toURL)
          else {
            // No scheme => treat as filesystem path and require it to exist.
            val file = new File(loc)
            if (file.exists()) Some(file.toURI.toURL) else None
          }
        } catch {
          case _: Exception =>
            // Fallback: treat as filesystem path if valid & exists.
            val file = new File(loc)
            if (file.exists()) Some(file.toURI.toURL) else None
        }
      }
    }
  }
}
