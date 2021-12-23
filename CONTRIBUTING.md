# Contributing Guide

## Introduction

The team gratefully accepts contributions via [pull requests](https://help.github.com/articles/about-pull-requests/).

## Tests

The project has integration tests based on `scripted test framework`.
See https://www.scala-sbt.org/1.x/docs/Testing-sbt-plugins.html
Command to run the tests: `sbt ';sbt-bom / scripted'`

## Coding Standards

Styles conventions:

- Each Scala class should have a **Copyright Notice**:
```text
/*
 * Copyright (C) 20<x>-20<y> HERE Europe B.V.
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
```
replace the `<x>` and `<y>` with numbers to denote the years in which the materials were created and modified.
- The package name should start with `com.here`
- The folder structure should reflect the package name
- Basic Scala stylistic guidelines can be found [here](https://docs.scala-lang.org/style/).
- It is recommended to use [Scalafmt](https://scalameta.org/scalafmt/) code formatter. The configuration is in `.scalafmt.conf` file.

You may use [IntelliJ IDEA scalfmt plugin](https://plugins.jetbrains.com/plugin/8236-scalafmt)
or the following SBT commands available in the project:

* `scalafmtSrcAll` - formats compile sources, test sources and sbt sources
* `scalafmt` - formats compile sources.
* `scalafmtSbt` - formats sbt sources.
* `scalafmtCheckAll` - checks that all project sources are properly formatted and fails otherwise.

# Commit Signing

As part of filing a pull request we ask you to sign off the
[Developer Certificate of Origin](https://developercertificate.org/) (DCO) in each commit.
Any Pull Request with commits that are not signed off will be reject by the
[DCO check](https://probot.github.io/apps/dco/).

A DCO is lightweight way for contributors to confirm that they wrote or otherwise have the right
to submit code or documentation to a project. Simply add `Signed-off-by` as shown in the example below
to indicate that you agree with the DCO.

An example signed commit message:

```
    README.md: Fix minor spelling mistake

    Signed-off-by: John Doe <john.doe@example.com>
```

Git has the `-s` flag that can sign a commit for you, see example below:

`$ git commit -s -m 'README.md: Fix minor spelling mistake'`

# GitHub Actions
All opened pull requests are tested by GitHub Actions before they can be merged into the target branch.
After the new code is pushed to `master` GitHub Actions will run the test suite again, build the artifacts and release them
to Maven Central repository. The job will automatically increase patch version during this process.
If you do not want your changes to trigger a release, add the `[skip release]` flag to your commit message,
e.g., `git commit -s -m "[skip release] Fixed proxy configuration"`. We recommend this for example when you update
CI scripts or documentation such as README.md and CONTRIBUTING.md.