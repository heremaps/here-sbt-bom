#!/bin/bash

#
# Copyright (C) 2017-2022 HERE Global B.V. and its affiliate(s).
# All rights reserved.
#
# This software and other materials contain proprietary information
# controlled by HERE and are protected by applicable copyright legislation.
# Any use and utilization of this software and other materials and
# disclosure to any third parties is conditional upon having a separate
# agreement with HERE for the access, use, utilization or disclosure of this
# software. In the absence of such agreement, the use of the software is not
# allowed.
#


COMMIT_MESSAGE=$(git log -1 --pretty=format:%s)
LATEST_TAG=$1

TAG_SEMVER=(${LATEST_TAG//./ })
MAJOR="${TAG_SEMVER[0]}"
MINOR="${TAG_SEMVER[1]}"
PATCH="${TAG_SEMVER[2]}"

if [[ ${COMMIT_MESSAGE} =~ .*([Bb]reaking).* ]]; then
  MAJOR=$((MAJOR + 1))
  MINOR=1
  PATCH=1
elif [[ ${COMMIT_MESSAGE} =~ .*([Ff]eature).* ]]; then
  MINOR=$((MINOR + 1))
  PATCH=1
else
  PATCH=$((PATCH + 1))
fi

RELEASE_TAG="${MAJOR}.${MINOR}.${PATCH}"

git tag -a "${RELEASE_TAG}" -m "${RELEASE_TAG}"
git push --tags

echo "${RELEASE_TAG}"

