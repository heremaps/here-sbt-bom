/*
 * Copyright (C) 2019-2023 HERE Europe B.V.
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

import sbt.File
import sbt.util.Logger

import java.io.FileInputStream
import java.util.Properties
import scala.collection.JavaConverters._

object IvyPomLocator {
  /*
     Ivy internals require ivy.home to be absolute.
     Example of error if this doesn't hold:

     java.lang.IllegalArgumentException: ivy.home must be absolute: .sbt-cache/ivy2
            at org.apache.ivy.util.Checks.checkAbsolute(Checks.java:57)
            at org.apache.ivy.core.settings.IvySettings.getDefaultIvyUserDir(IvySettings.java:801)
            at org.apache.ivy.core.settings.IvySettings.getDefaultCache(IvySettings.java:824)
            at org.apache.ivy.core.settings.IvySettings.getDefaultResolutionCacheBasedir(IvySettings.java:864)
            at sbt.internal.librarymanagement.IvySbt$.$anonfun$configureResolutionCache$1(Ivy.scala:582)
   */
  def tweakIvyHome(logger: Logger): Unit = {
    val ivyHome = System.getProperty("ivy.home")
    if (ivyHome != null) {
      val absHome = new File(ivyHome).getAbsolutePath
      if (System.setProperty("ivy.home", absHome) != absHome) {
        logger.warn(s"Adjusting ivy.home: $ivyHome -> $absHome")
      }
    }
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
        resolver.wrapDependencyInModule(moduleId.asModule()),
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

  def findLocalPomFile(moduleId: NormalizedArtifact): Option[File] = {
    /*
    If the artifact was resolved by Ivy, the pom file will be stored under .ivy2 directory.
    But if it was previously resolved by Maven, Ivy will not download it second time and just store the reference
    Original POM file could be under either .ivy or .m2 directory:

    Examples:
    1) Pom file under .ivy directory:
        $ ~/s/vsvu-utils (batch-support)> cat ~/.ivy2/cache/com.here.platform/sdk-dep-common_2.12/ivydata-1.0.46.properties
        #ivy cached data file for com.here.platform#sdk-dep-common_2.12;1.0.46
        #Mon Jan 17 12:20:26 CET 2022
        artifact\:sdk-dep-common_2.12\#pom.original\#pom\#-691773007.location=~/.m2/repository/com/here/platform/sdk-dep-common_2.12/1.0.46/sdk-dep-common_2.12-1.0.46.pom
        artifact\:sdk-dep-common_2.12\#pom.original\#pom\#-691773007.is-local=true
        artifact\:sdk-dep-common_2.12\#pom.original\#pom\#-691773007.exists=true
        artifact\:ivy\#ivy\#xml\#-1235680096.exists=true
        artifact\:ivy\#ivy\#xml\#-1235680096.location=~/.m2/repository/com/here/platform/sdk-dep-common_2.12/1.0.46/sdk-dep-common_2.12-1.0.46.pom
        resolver=sbt-chain
        artifact\:ivy\#ivy\#xml\#-1235680096.is-local=true
     2) Pom file downloaded by Maven first:
        $ ~/s/vsvu-utils (batch-support)> cat ~/.ivy2/cache/com.here.platform/sdk-dep-common_2.12/ivydata-1.0.47.properties
        #ivy cached data file for com.here.platform#sdk-dep-common_2.12;1.0.47
        #Thu Jan 06 12:38:21 CET 2022
        artifact\:sdk-dep-common_2.12\#pom.original\#pom\#-691766754.is-local=false
        artifact\:sdk-dep-common_2.12\#pom.original\#pom\#-691766754.exists=true
        artifact\:ivy\#ivy\#xml\#-1235673843.exists=true
        artifact\:ivy\#ivy\#xml\#-1235673843.location=https\://artifactory.in.here.com/artifactory/here-olp-sit/com/here/platform/sdk-dep-common_2.12/1.0.47/sdk-dep-common_2.12-1.0.47.pom
        artifact\:ivy\#ivy\#xml\#-1235673843.is-local=false
        artifact\:sdk-dep-common_2.12\#pom.original\#pom\#-691766754.location=https\://artifactory.in.here.com/artifactory/here-olp-sit/com/here/platform/sdk-dep-common_2.12/1.0.47/sdk-dep-common_2.12-1.0.47.pom
     */
    val localIvyPomLocation = new File(
      ivyHome,
      s"/cache/${moduleId.group}/${moduleId.name}/ivy-${moduleId.version}.xml.original"
    )
    if (localIvyPomLocation.exists()) {
      Some(localIvyPomLocation)
    } else {
      val ivyPropsFile = new File(
        ivyHome,
        s"/cache/${moduleId.group}/${moduleId.name}/ivydata-${moduleId.version}.properties"
      )
      referencedPomLocation(ivyPropsFile)
    }
  }

  private def referencedPomLocation(ivyPropertyFile: File): Option[File] = {
    val properties = new Properties()
    if (ivyPropertyFile.exists()) {
      val is = new FileInputStream(ivyPropertyFile)
      try {
        properties.load(is)
      } finally {
        is.close()
      }

      val locationKey =
        properties.keys().asScala.find(key => key.asInstanceOf[String].endsWith(".location"))
      locationKey.map(k => {
        val f = new File(properties.getProperty(k.asInstanceOf[String]))
        require(f.exists(), s"File $f doesn't exist, is artifact cache corrupted?")
        f
      })
    } else None
  }
}
