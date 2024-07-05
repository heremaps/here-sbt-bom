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

import com.here.bom.Bom
import org.apache.ivy.core.IvyPatternHelper
import org.apache.ivy.plugins.parser.m2.PomReader
import org.apache.ivy.plugins.repository.url.URLResource
import sbt.librarymanagement.DependencyBuilders.OrganizationArtifactName
import sbt._

import scala.annotation.tailrec
import scala.collection.JavaConverters._

/**
 * A class for reading Bill of Materials (BOM).
 *
 * @param pomLocator
 *   The IvyPomLocator used to locate POM files.
 * @param logger
 *   The Logger used for logging messages.
 * @param scalaBinaryVersion
 *   The Scala binary version to use for dependency resolution.
 */
class BomReader(pomLocator: IvyPomLocator, logger: Logger, scalaBinaryVersion: String) {
  type Priority = Int
  type Props = java.util.HashMap[String, String]

  // Parsed pom file of a module.
  // `effectiveProps` contain all properties including properties from parents.
  case class ResolvedBom(moduleID: NormalizedArtifact, reader: PomReader, effectiveProps: Props)

  private case class ManagedDeps(
      plainArtifacts: Seq[NormalizedArtifact],
      boms: Seq[NormalizedArtifact]
  )

  // For some reason variable substitution in PomReader doesn't work out of the box
  // And sometimes we want to evaluate it using modified property map
  private def eval(expr: String, properties: Props): Option[String] = {
    val res = IvyPatternHelper.substituteVariables(expr, properties)
    if (res.contains("${")) {
      logger.warn(s"Failed to resolve $expr. Ignoring this element.")
      None
    } else {
      Some(res)
    }
  }

  private def readPom(moduleId: NormalizedArtifact): PomReader = {
    val pomFile = pomLocator
      .getPomFile(moduleId)
      .getOrElse(sys.error(s"Failed to resolve ${moduleId}"))
    val url = pomFile.asURL
    logger.debug(f"Reading pom file $url")
    new PomReader(url, new URLResource(url))
  }

  def makeBom(bomArtifact: ModuleID): Bom = {
    // Avoid unbounded non-tail recursion
    @tailrec
    def extractRecursively(
        rawBoms: List[(NormalizedArtifact, Priority)],
        resolvedBoms: List[(ResolvedBom, Priority)],
        visitedBoms: Set[NormalizedArtifact],
        resultAcc: Seq[(NormalizedArtifact, Priority)]
    ): Seq[(NormalizedArtifact, Priority)] = {
      resolvedBoms match {
        case Nil => {
          rawBoms match {
            case Nil => resultAcc
            case (bom, prio) :: otherBoms => {
              val resolved = readPomAndParents(bom, prio)
              extractRecursively(otherBoms, resolved, visitedBoms, resultAcc)
            }
          }
        }
        case (ResolvedBom(m, reader, props), prio) :: otherBoms => {
          val deps = readManagedDeps(reader, props)
          val newBoms = deps.boms.filter(!visitedBoms.contains(_))
          val newVisited = visitedBoms + m
          val newArtifacts = deps.plainArtifacts.map(v => (v, prio))
          extractRecursively(
            rawBoms ++ newBoms.map(v => (v, prio + 1)),
            otherBoms,
            newVisited,
            resultAcc ++ newArtifacts
          )
        }
      }
    }

    val versions: Seq[(NormalizedArtifact, Priority)] = extractRecursively(
      List((NormalizedArtifact.fromModule(bomArtifact, scalaBinaryVersion), 0)),
      Nil,
      Set.empty,
      Nil
    )
    assembleBom(versions)
  }

  private def readManagedDeps(reader: PomReader, properties: Props): ManagedDeps = {
    val versions = reader.getDependencyMgt.asScala
      .flatMap(el => {
        val dme = el.asInstanceOf[reader.PomDependencyMgtElement]
        for {
          group <- eval(dme.getGroupId, properties)
          artifact <- eval(dme.getArtifactId, properties)
          version <- eval(dme.getVersion, properties)
          isBom = "import" == dme.getScope
        } yield (NormalizedArtifact(group, artifact, version), isBom)
      })
      .toVector

    val (poms, deps) = versions.partition(_._2)
    ManagedDeps(deps.map(_._1), poms.map(_._1))
  }

  private def assembleBom(artifacts: Seq[(NormalizedArtifact, Priority)]): Bom = {
    def chooseBestVersion(versions: Seq[(NormalizedArtifact, Priority)]): String = {
      val best = versions.minBy(_._2)._1
      val bestVersion = best.version
      val evicted = versions.map(_._1.version).filterNot(_ == bestVersion)
      if (evicted.nonEmpty) {
        logger.info(s"$best. Evicting versions ${evicted.mkString(", ")}")
      }
      bestVersion
    }

    val effectiveVersions: Map[(String, String), String] = artifacts
      .groupBy(e => (e._1.group, e._1.name))
      .mapValues(chooseBestVersion)

    logger.debug(s"Effective resolved versions: $effectiveVersions")

    new Bom {
      override def version(dependency: OrganizationArtifactName): String = {
        val normalized = NormalizedArtifact.fromModule(dependency % "whatever", scalaBinaryVersion)
        effectiveVersions
          .get((normalized.group, normalized.name))
          .getOrElse(
            sys.error(s"Version for ${normalized.group}.${normalized.name} not found in BOM")
          )
      }

      override def bomDependencies: Seq[ModuleID] = {
        effectiveVersions.map { case ((group, name), version) =>
          ModuleID(group, name, version)
        }.toSeq
      }

    }
  }

  private def readPomAndParents(
      module: NormalizedArtifact,
      priority: Priority
  ): List[(ResolvedBom, Priority)] = {
    val chain = buildParentsChain(module)
    logger.debug(f"Resolved parents chain for the module: $module: $chain")
    val rootProps = new Props()
    rootProps.put("scala.compat.version", scalaBinaryVersion)
    val rootPriority = priority + chain.size - 1

    @tailrec
    def attachInheritedProps(
        rest: List[(NormalizedArtifact, PomReader)],
        acc: List[(ResolvedBom, Priority)],
        cumulativeProps: Props,
        prio: Priority
    ): List[(ResolvedBom, Priority)] = {
      rest match {
        case Nil => acc
        case (m, reader) :: children => {
          val updatedProps = mergeProperties(reader, cumulativeProps)
          val res = (ResolvedBom(m, reader, updatedProps), prio)
          attachInheritedProps(children, res :: acc, updatedProps, prio - 1)
        }
      }
    }

    val tuples = attachInheritedProps(chain, Nil, rootProps, rootPriority)
    logger.debug(f"Inherited properties: $rootProps")
    tuples
  }

  private def mergeProperties(reader: PomReader, props: Props): Props = {
    val into = new Props(props)
    into.putAll(reader.getPomProperties.asInstanceOf[Props])
    into.put("project.version", reader.getVersion)
    into.put("project.groupId", reader.getGroupId)
    into.put("project.artifactId", reader.getArtifactId)
    into.put("project.packaging", reader.getPackaging)
    into.put("project.description", reader.getDescription)
    into
  }

  private def buildParentsChain(
      module: NormalizedArtifact
  ): List[(NormalizedArtifact, PomReader)] = {
    @tailrec
    def go(
        current: NormalizedArtifact,
        acc: List[(NormalizedArtifact, PomReader)]
    ): List[(NormalizedArtifact, PomReader)] = {
      if (acc.exists(_._1 == current)) {
        sys.error(s"Parents of ${current} form a cycle")
      }
      val reader = readPom(current)

      if (reader.hasParent) {
        val props = new Props(reader.getPomProperties.asInstanceOf[Props])
        props.putIfAbsent("scala.compat.version", scalaBinaryVersion)

        def evalOrFail(expr: String): String = {
          eval(expr, props).getOrElse(
            sys.error(s"Failed to eval expr ${expr} in $module. Known props: $props")
          )
        }

        val parent = NormalizedArtifact(
          evalOrFail(reader.getParentGroupId),
          evalOrFail(reader.getParentArtifactId),
          evalOrFail(reader.getParentVersion)
        )

        logger.debug(f"Module $module has parent $parent")

        go(parent, (current, reader) :: acc)
      } else {
        (current, reader) :: acc
      }
    }

    go(module, Nil)
  }
}
