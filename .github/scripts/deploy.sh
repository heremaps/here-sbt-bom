#!/bin/bash
set -ev

if [[ $(git log -1 --pretty=format:"%s") =~ "[skip release]" ]]; then
  echo 'Stopping the script because [skip release] was found in commit message'
  exit 0
fi

export GPG_TTY=$(tty)
export SONATYPE_USERNAME=$OSSRH_USERNAME
export SONATYPE_PASSWORD=$OSSRH_PASSWORD
export PGP_PASSPHRASE=$GPG_PASSPHRASE

gpg --version

# Import gpg key
echo $GPG_PRIVATE_KEY | base64 -d > private.key
gpg --import --batch private.key

# Deploy to Maven Central
sbt sonatypeDropAll
sbt publishSigned
sbt sonatypeRelease
