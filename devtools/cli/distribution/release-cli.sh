#!/bin/bash
set -e -o pipefail

VERSION=$1
if [ -z "$VERSION" ]
then
    echo "Must specify Quarkus version"
    exit 1
fi

BRANCH=$2
if [ -z "$BRANCH" ]
then
    echo "Must specify Quarkus branch"
    exit 1
fi

MAINTENANCE="$3"
if [ -z "$MAINTENANCE" ]
then
    echo "Must specify maintenance mode"
    exit 1
fi

PREVIEW="$4"
if [ -z "$PREVIEW" ]
then
    echo "Must specify preview mode"
    exit 1
fi

# GitHub releases are immutable once published so publishing the CLI is done in two steps:
# - release: creates the archives and a draft GitHub release with the archives attached
# - publish: once the GitHub release has been published, publishes the packages (SDKMAN, Homebrew, Chocolatey)
#   that point to the archives of the GitHub release
MODE="$5"
if [ "$MODE" != "release" ] && [ "$MODE" != "publish" ]
then
    echo "Must specify mode: release or publish"
    exit 1
fi

JRELEASER_VERSION=1.27.0

DIST_DIR="$( dirname "${BASH_SOURCE[0]}" )"
pushd ${DIST_DIR}

echo "Cleaning up target"
rm -rf target
mkdir -p target

JAVA_BINARY_DIR=quarkus-cli-${VERSION}
RUNNER_JAR=quarkus-cli-${VERSION}-runner.jar

if [ "$MODE" == "release" ]; then
    echo "Copying java-binary structure to target/${JAVA_BINARY_DIR}"
    cp -ap java-binary target/${JAVA_BINARY_DIR}
    sed -i.bak "s/__RUNNER_JAR__/${RUNNER_JAR}/" target/${JAVA_BINARY_DIR}/bin/quarkus
    sed -i.bak "s/__RUNNER_JAR__/${RUNNER_JAR}/" target/${JAVA_BINARY_DIR}/bin/quarkus.bat
    rm -f target/${JAVA_BINARY_DIR}/bin/*.bak

    echo "Downloading CLI runner jar into target/${JAVA_BINARY_DIR}/lib/"

    mkdir target/${JAVA_BINARY_DIR}/lib
    curl -f -S -s -o target/${JAVA_BINARY_DIR}/lib/${RUNNER_JAR} \
        https://repo1.maven.org/maven2/io/quarkus/quarkus-cli/${VERSION}/${RUNNER_JAR}

    echo "Creating archives"
    pushd target
    zip -r ${JAVA_BINARY_DIR}.zip ${JAVA_BINARY_DIR}
    tar -zcf ${JAVA_BINARY_DIR}.tar.gz ${JAVA_BINARY_DIR}
    popd
else
    # the packages contain the checksums of the archives so we need the exact archives attached to the GitHub release
    echo "Downloading archives from the GitHub release"
    for ARCHIVE in ${JAVA_BINARY_DIR}.zip ${JAVA_BINARY_DIR}.tar.gz; do
        curl -f -S -s -L -o target/${ARCHIVE} \
            https://github.com/quarkusio/quarkus/releases/download/${VERSION}/${ARCHIVE}
    done
fi

CONFIG_FILE="jreleaser.yml"
export JRELEASER_PROJECT_VERSION=${VERSION}
export JRELEASER_BRANCH=${BRANCH}
if [ "$MAINTENANCE" == "true" ]; then
    CONFIG_FILE="jreleaser-maintenance.yml"
    export JRELEASER_CHOCOLATEY_GITHUB_BRANCH=${BRANCH}
    export JRELEASER_HOMEBREW_GITHUB_BRANCH=${BRANCH}
fi
if [ "$PREVIEW" == "true" ]; then
    CONFIG_FILE="jreleaser-preview.yml"
fi

# the SDKMAN packager also takes care of the SDKMAN announcement so we don't need the announce command
JRELEASER_COMMAND="$MODE"

jbang org.jreleaser:jreleaser:${JRELEASER_VERSION} ${JRELEASER_COMMAND} \
  -c ${CONFIG_FILE} \
  --git-root-search \
  --output-directory target 2>&1 | tee target/jreleaser-${JRELEASER_COMMAND}.log

# JReleaser only logs a warning when it fails to update a package repository (Chocolatey is allowed to fail).
# Depending on when it fails, the repository is logged either as quarkusio/homebrew-tap or as its URL.
if grep -q "Unexpected error updating .*homebrew-tap" target/jreleaser-${JRELEASER_COMMAND}.log; then
    echo "JReleaser failed to update the Homebrew tap, see the log above"
    exit 1
fi

popd
