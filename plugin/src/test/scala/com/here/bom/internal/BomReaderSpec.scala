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

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalamock.scalatest.MockFactory
import sbt.{Logger, ModuleID}

import java.io.File
import sbt._

class BomReaderSpec extends AnyFlatSpec with Matchers with MockFactory {

  "BomReader" should "assemble the BOM correctly" in {
    val pomLocatorMock = mock[IvyPomLocator]
    val loggerMock = mock[Logger]
    val scalaBinaryVersion = "2.12"

    val bomReader = new BomReader(pomLocatorMock, loggerMock, scalaBinaryVersion)

    (pomLocatorMock.getPomFile _)
      .expects(*)
      .returns(Some(createMockPomFile("com.test", "test_bom", "1.0", false)))

    val bom = bomReader.makeBom(ModuleID("com.test", "test_bom", "1.0"))
    bom should not be (null)

    val dependency = "com.test" %% "test_module" % bom
    dependency.revision should be("1.0")
  }

  "BomReader" should "assemble the BOM correctly with parent" in {
    val pomLocatorMock = mock[IvyPomLocator]
    val loggerMock = mock[Logger]
    val scalaBinaryVersion = "2.12"

    val bomReader = new BomReader(pomLocatorMock, loggerMock, scalaBinaryVersion)

    (pomLocatorMock.getPomFile _)
      .expects(where((moduleId: NormalizedArtifact) => {
        moduleId.group.equals("com.test") && moduleId.name.equals("test_bom_parent") && moduleId.version.equals("1.0")
      }))
      .returns(Some(createMockPomFile("com.test", "test_bom_parent", "1.0", false)))

    (pomLocatorMock.getPomFile _)
      .expects(where((moduleId: NormalizedArtifact) => {
        moduleId.group.equals("com.test") && moduleId.name.equals("test_bom") && moduleId.version.equals("1.0")
      }))
      .returns(Some(createMockPomFile("com.test", "test_bom", "1.0", true)))

    val bom = bomReader.makeBom(ModuleID("com.test", "test_bom", "1.0"))
    bom should not be (null)

    val dependency = "com.test" %% "test_module" % bom
    dependency.revision should be("1.0")
  }

  private def createMockPomFile(groupId: String, artifactId: String, version: String, addParent: Boolean): File = {
    val tempFile = File.createTempFile("mock", ".xml")

    val pomContent =
      s"""<?xml version="1.0" encoding="UTF-8"?>
         |<project>
         |  <groupId>$groupId</groupId>
         |  <artifactId>$artifactId</artifactId>
         |  <version>$version</version>
         |  ${
        if (addParent)
          s"""
             |  <parent>
             |    <groupId>$groupId</groupId>
             |    <artifactId>${artifactId}_parent</artifactId>
             |    <version>$version</version>
             |  </parent>
             |""".stripMargin
        else
          s"""
             |<dependencyManagement>
             |         |    <dependencies>
             |         |      <dependency>
             |         |        <groupId>com.test</groupId>
             |         |        <artifactId>test_module_2.12</artifactId>
             |         |        <version>1.0</version>
             |         |      </dependency>
             |         |    </dependencies>
             |</dependencyManagement>
             |""".stripMargin
      }
         |</project >
         | """.stripMargin

    val writer = new java.io.FileWriter(tempFile)
    writer.write(pomContent)
    writer.close()

    tempFile
  }
}
