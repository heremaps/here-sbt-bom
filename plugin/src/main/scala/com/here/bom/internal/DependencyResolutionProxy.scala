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
import sbt.librarymanagement.DependencyResolution
import sbt.librarymanagement._
import sbt.util.Logger
import java.io.File

class DependencyResolutionProxy(delegate: DependencyResolution) {

  def moduleDescriptor(moduleSetting: ModuleDescriptorConfiguration): ModuleDescriptor =
    delegate.moduleDescriptor(moduleSetting)

  def moduleDescriptor(
      moduleId: ModuleID,
      directDependencies: Vector[ModuleID],
      scalaModuleInfo: Option[ScalaModuleInfo]
  ): ModuleDescriptor = {
    delegate.moduleDescriptor(moduleId, directDependencies, scalaModuleInfo)
  }

  def update(
      module: ModuleDescriptor,
      configuration: UpdateConfiguration,
      uwconfig: UnresolvedWarningConfiguration,
      log: Logger
  ): Either[UnresolvedWarning, UpdateReport] =
    delegate.update(module, configuration, uwconfig, log)

  def wrapDependencyInModule(dependencyId: ModuleID): ModuleDescriptor =
    delegate.wrapDependencyInModule(dependencyId)

  def wrapDependencyInModule(
      dependencyId: ModuleID,
      scalaModuleInfo: Option[ScalaModuleInfo]
  ): ModuleDescriptor = {
    delegate.wrapDependencyInModule(dependencyId, scalaModuleInfo)
  }

  def retrieve(
      dependencyId: ModuleID,
      scalaModuleInfo: Option[ScalaModuleInfo],
      retrieveDirectory: File,
      log: Logger
  ): Either[UnresolvedWarning, Vector[File]] =
    delegate.retrieve(dependencyId, scalaModuleInfo, retrieveDirectory, log)

  def retrieve(
      module: ModuleDescriptor,
      retrieveDirectory: File,
      log: Logger
  ): Either[UnresolvedWarning, Vector[File]] = {
    delegate.retrieve(module, retrieveDirectory, log)
  }

  def updateClassifiers(
      config: GetClassifiersConfiguration,
      uwconfig: UnresolvedWarningConfiguration,
      artifacts: Vector[(String, ModuleID, Artifact, File)],
      log: Logger
  ): Either[UnresolvedWarning, UpdateReport] = {
    delegate.updateClassifiers(config, uwconfig, artifacts, log)
  }

}
