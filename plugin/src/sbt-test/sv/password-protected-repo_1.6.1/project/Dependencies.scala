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
import sbt._
import com.here.bom.Bom

object Dependencies {
  lazy val additionalResolvers = Seq(
    "HERE_PLATFORM_REPO" at "https://repo.platform.here.com/artifactory/open-location-platform"
  )
}

case class Dependencies(platformBom: Bom) {
  val dependencies: Seq[ModuleID] = Seq(
    "org.apache.flink" %% "flink-scala" % platformBom,
    "org.apache.flink" %% "flink-streaming-scala" % platformBom,
    "com.here.platform.data.client" %% "flink-support" % platformBom,
    "com.here.platform.pipeline" %% "pipeline-interface" % platformBom,
  )
}