#!/bin/bash
VERSION=$1
if [ -z "$VERSION" ]
then
    echo "Must specify Quarkus version"
    exit 1
fi

DIST_DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
cd ${DIST_DIR}

echo "Downloading CLI runner jar into ${DIST_DIR}/target"
mkdir -p ${DIST_DIR}/target

# Download the published runner jar & checksums (bail if it fails)
curl -S -s -o ${DIST_DIR}/target/quarkus-cli-${VERSION}-runner.jar \
    https://repo1.maven.org/maven2/io/quarkus/quarkus-cli/${VERSION}/quarkus-cli-${VERSION}-runner.jar      || exit
curl -S -s -o ${DIST_DIR}/target/quarkus-cli-${VERSION}-runner.jar.asc \
    https://repo1.maven.org/maven2/io/quarkus/quarkus-cli/${VERSION}/quarkus-cli-${VERSION}-runner.jar.asc  || exit
curl -S -s -o ${DIST_DIR}/target/quarkus-cli-${VERSION}-runner.jar.md5 \
    https://repo1.maven.org/maven2/io/quarkus/quarkus-cli/${VERSION}/quarkus-cli-${VERSION}-runner.jar.md5  || exit
curl -S -s -o ${DIST_DIR}/target/quarkus-cli-${VERSION}-runner.jar.sha1 \
    https://repo1.maven.org/maven2/io/quarkus/quarkus-cli/${VERSION}/quarkus-cli-${VERSION}-runner.jar.sha1 || exit


export JRELEASER_PROJECT_VERSION=${VERSION}
export JRELEASER_BRANCH=main

# TODO: Snapshot release --git-root-search support
jbang jreleaser-snapshot@jreleaser full-release -y \
  --git-root-search \
  -od target
