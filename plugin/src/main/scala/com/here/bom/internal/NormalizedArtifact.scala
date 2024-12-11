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

import sbt._

/**
 * ModuleId may conduct information about scala version either directly as a part of artifact name
 * or through CrossVersion property. `NormailzedArtifact` puts the suffix to the artifact name.
 */
object NormalizedArtifact {
  def fromModule(m: ModuleID, scalaBinaryVersion: String): NormalizedArtifact = {
    m.crossVersion match {
      case Disabled => NormalizedArtifact(m.organization, m.name, m.revision)
      case _ => NormalizedArtifact(m.organization, s"${m.name}_$scalaBinaryVersion", m.revision)
    }
  }
}

case class NormalizedArtifact(group: String, name: String, version: String) {
  def asModule(): ModuleID = group % name % version
}
