# Contributing Guide

## Introduction

The team gratefully accepts contributions via [pull requests](https://help.github.com/articles/about-pull-requests/).

## Build
To build the project run the command: `sbt package`.
To publish the plugin into the local Maven repository run the command `sbt publishM2`. The plugin will be published with the `0.0.0-SNAPSHOT` version.

## Run Tests

### Unit tests

The project has unit tests based on `scalatest` and `scalamock`. To run the tests run the command: `sbt test`.

### Integration Tests

The project has integration tests based on a `scripted test framework`.
See https://www.scala-sbt.org/1.x/docs/Testing-sbt-plugins.html
Tests split into 2 groups
- `psv` - run on any branch. These tests group **mustn't** rely on the secrets
- `sv` - run only on master.
Command to run the psv acceptance tests: `sbt ';sbt-bom / scripted psv/*'`

## Continuous Integration

The CI is run on GitHub, meaning that files in the `.github` folder are used to define the workflows.

### Presubmit Verification
The purpose of this verification is to do a check that the code is not broken.

The presubmit verification does the following:

#### `Check` workflow

##### `sbt-header-check` step
This step checks the availability of the appropriate license in all `.scala` files in the project. This step fails if the license in some files contains an inappropriate license or is absent.
If the step fails, set the appropriate license in these source files. An appropriate license can be found in the [LICENSE](LICENSE) file.

##### `sbt-scala-formatter-check` step
This step checks the code style in all `.scala` and `.sbt` files in the project. This job fails if the code is not formatted properly.

#### `Test` workflow

##### `scripted-test-psv` step
This step runs integration tests located in the `plugin/src/sbt-test/psv` folder

##### `test` step
This step runs unit tests and evaluates test coverage

#### `update-version` step
This step updates the version in `version.sbt` with value from the latest git tag

#### `package` step
This step packages the plugin into the jar file.

### Submit Verification
The purpose of this workflow is to verify that the `master` branch is always in the `ready for a deploy`
state and release the plugin into the [Maven Central repository](https://repo.maven.apache.org/maven2/com/here/platform/sbt-bom_2.12_1.0/).

The submit verification runs all the [Presubmit Verification](#presubmit-verification) workflows with the following additional steps:
#### `Test` workflow

##### `scripted-test-sv` step
This step runs integration tests located in the `plugin/src/sbt-test/sv` folder

#### `Release` workflow

##### `Push git tag` step
The step increments the current version and pushes a new git tag

##### `Deploy` step
This step releases the plugin to the [Maven Central repository](https://repo.maven.apache.org/maven2/com/here/platform/sbt-bom_2.12_1.0/).

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