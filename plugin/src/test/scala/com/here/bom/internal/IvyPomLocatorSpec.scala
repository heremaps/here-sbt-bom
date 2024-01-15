/*
 * Copyright (C) 2019-2024 HERE Europe B.V.
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

import org.scalatest.matchers.should.Matchers
import org.scalamock.scalatest.MockFactory
import org.scalatest.flatspec.AnyFlatSpec
import sbt.librarymanagement.{ModuleDescriptor, ModuleID, ResolveException, UnresolvedWarning}

import java.io.File

import java.nio.file.Files

class IvyPomLocatorSpec extends AnyFlatSpec with Matchers with MockFactory {

  "getPomFile" should "return the local xml.original file" in {
    val resolverMock = mock[DependencyResolutionProxy]
    val ivyHomeTemp = Files.createTempDirectory("sbt-bom-tmp")
    val bomXml = ivyHomeTemp.toAbsolutePath.resolve("cache").resolve("com.test").resolve("test_bom").toFile
    bomXml.mkdirs()
    bomXml.toPath.resolve("ivy-1.0.xml.original").toFile.createNewFile()
    val loggerMock = stub[sbt.Logger]

    val ivyPomLocator = new IvyPomLocator(resolverMock, ivyHomeTemp.toFile, loggerMock)

    val moduleId = NormalizedArtifact("com.test", "test_bom", "1.0")

    val pomFilePath = ivyPomLocator.getPomFile(moduleId).get.getAbsolutePath

    pomFilePath should include("cache/com.test/test_bom/ivy-1.0.xml.original")
  }

  "getPomFile" should "return the local .properties file" in {
    val resolverMock = mock[DependencyResolutionProxy]
    val ivyHomeTemp = Files.createTempDirectory("sbt-bom-tmp")
    val bomPropertiesFolder = ivyHomeTemp.toAbsolutePath.resolve("cache").resolve("com.test").resolve("test_bom").toFile
    bomPropertiesFolder.mkdirs()
    val bomProperties = bomPropertiesFolder.toPath.resolve("ivydata-1.0.properties")
    bomProperties.toFile.createNewFile()
    val pomFilePath = Files.createTempFile("sbt-bom-test", "")
    Files.write(bomProperties, ("artifact\\:test_bom\\#pom.original\\#pom\\#459090996.location=" + pomFilePath).getBytes)
    val loggerMock = stub[sbt.Logger]

    val ivyPomLocator = new IvyPomLocator(resolverMock, ivyHomeTemp.toFile, loggerMock)

    val moduleId = NormalizedArtifact("com.test", "test_bom", "1.0")

    ivyPomLocator.getPomFile(moduleId).get.getAbsolutePath should be(pomFilePath.toString)
  }

  "getPomFile" should "return the local xml.original file from module" in {
    val resolverMock = mock[DependencyResolutionProxy]
    val moduleDescriptorMock = mock[ModuleDescriptor]
    val ivyHomeTemp = Files.createTempDirectory("sbt-bom-tmp")
    (resolverMock.wrapDependencyInModule(_: sbt.librarymanagement.ModuleID)).expects(*).onCall((moduleId) => {
      val bomXml = ivyHomeTemp.toAbsolutePath.resolve("cache").resolve("com.test").resolve("test_bom").toFile
      bomXml.mkdirs()
      bomXml.toPath.resolve("ivy-1.0.xml.original").toFile.createNewFile()
      moduleDescriptorMock
    })
    (resolverMock.retrieve(_: ModuleDescriptor, _: File, _: sbt.Logger)).expects(*, *, *).returning(Left(UnresolvedWarning(new ResolveException(Seq("test message"), Seq(ModuleID("com.test", "test_bom", "1.0"))), null)))

    val loggerMock = stub[sbt.Logger]

    val ivyPomLocator = new IvyPomLocator(resolverMock, ivyHomeTemp.toFile, loggerMock)

    val moduleId = NormalizedArtifact("com.test", "test_bom", "1.0")

    ivyPomLocator.getPomFile(moduleId).get.getAbsolutePath should include("cache/com.test/test_bom/ivy-1.0.xml.original")
  }

}

