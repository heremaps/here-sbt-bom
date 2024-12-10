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
package com.here.bom

import com.here.bom.internal.{BomReader, DependencyResolutionProxy, IvyPomLocator}
import org.apache.ivy.util.url.CredentialsStore
import sbt.*
import sbt.Keys.*
import sbt.librarymanagement.DependencyBuilders.OrganizationArtifactName
import sbt.librarymanagement.ivy.IvyDependencyResolution

import java.util.concurrent.atomic.AtomicInteger

/**
 * Main class of the plugin. The `Bom` object provides methods for working with Bill of Materials
 * (BOM) in sbt projects.
 */
object Bom {
  private val counter = new AtomicInteger()

  def apply(bomArtifact: ModuleID): Def.Setting[Bom] = read(bomArtifact)(identity[Bom])

  def read[T: Manifest](bomArtifact: ModuleID)(extract: Bom => T): Def.Setting[T] = {
    val name = s"bom_${bomArtifact.toString}_${counter.getAndIncrement()}"
    val key = SettingKey[T](name)
    key := {
      val logger = (update / sLog).value
      val ivyConfig = InlineIvyConfiguration()
        .withResolvers(
          ((update / resolvers).value ++ (update / appResolvers).value.getOrElse(Seq.empty)).to
        )
        .withUpdateOptions((update / updateOptions).value)
      loadCredentials(logger)
      logger.debug(f"Ivy configuration used for the BOM dependency resolution: $ivyConfig")
      val depRes = new DependencyResolutionProxy(IvyDependencyResolution(ivyConfig))
      val ivyHome = (update / Keys.ivyPaths).value.ivyHome.get
      IvyPomLocator.tweakIvyHome(logger)
      val pomLocator = new IvyPomLocator(depRes, ivyHome, logger)
      val reader = new BomReader(pomLocator, logger, scalaBinaryVersion.value)
      val bom = reader.makeBom(bomArtifact)
      extract(bom)
    }
  }

  def dependencies(bomArtifact: ModuleID): Def.Setting[Seq[ModuleID]] = {
    read(bomArtifact)(bom => bom.bomDependencies)
  }

  implicit def addBomSyntax(dep: OrganizationArtifactName): BomSyntax = new BomSyntax(dep)

  class BomSyntax(dep: OrganizationArtifactName) {
    def %(bom: Bom): ModuleID = dep % bom.version(dep)
  }

  private def loadCredentials(logger: Logger): Unit = {
    val credentialFiles = Seq(
      Option(sys.props("sbt.boot.credentials"))
        .filter(s => s != null && s.nonEmpty)
        .map(new File(_)),
      sys.env.get("SBT_CREDENTIALS").map(new File(_)),
      Some(Path.userHome / ".ivy2" / ".credentials"),
      Some(Path.userHome / ".sbt" / ".credentials")
    )
    logger.debug(s"Credentials load order: $credentialFiles")
    val filePaths: Seq[File] = credentialFiles.flatten
    filePaths.find(_.exists()) match {
      case Some(file) =>
        logger.debug(s"Loading credentials from file $file")
        Credentials.loadCredentials(file) match {
          case Right(d) =>
            logger.debug(s"Credentials loaded from file $file")
            CredentialsStore.INSTANCE.addCredentials(d.realm, d.host, d.userName, d.passwd)
          case Left(error) => logger.warn(s"Failed to load credentials: $error")
        }
      case None =>
    }
  }
}

trait Bom {
  def version(dependency: OrganizationArtifactName): String

  /**
   * List of all dependencies declared in the BOM
   */
  def bomDependencies: Seq[ModuleID]
}
