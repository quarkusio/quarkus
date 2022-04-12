#!/bin/bash
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

DIST_DIR="$( dirname "${BASH_SOURCE[0]}" )"
pushd ${DIST_DIR}

echo "Cleaning up target"
rm -rf target
mkdir -p target

JAVA_BINARY_DIR=quarkus-cli-${VERSION}
RUNNER_JAR=quarkus-cli-${VERSION}-runner.jar

echo "Copying java-binary structure to target/${JAVA_BINARY_DIR}"
cp -ap java-binary target/${JAVA_BINARY_DIR}
sed -i.bak "s/__RUNNER_JAR__/${RUNNER_JAR}/" target/${JAVA_BINARY_DIR}/bin/quarkus
sed -i.bak "s/__RUNNER_JAR__/${RUNNER_JAR}/" target/${JAVA_BINARY_DIR}/bin/quarkus.bat
rm -f target/${JAVA_BINARY_DIR}/bin/*.bak

echo "Downloading CLI runner jar into target/${JAVA_BINARY_DIR}/lib/"

mkdir target/${JAVA_BINARY_DIR}/lib
curl -S -s -o target/${JAVA_BINARY_DIR}/lib/${RUNNER_JAR} \
    https://repo1.maven.org/maven2/io/quarkus/quarkus-cli/${VERSION}/${RUNNER_JAR} || exit

echo "Creating archives"
pushd target
zip -r ${JAVA_BINARY_DIR}.zip ${JAVA_BINARY_DIR}
tar -zcf ${JAVA_BINARY_DIR}.tar.gz ${JAVA_BINARY_DIR}
popd

export JRELEASER_PROJECT_VERSION=${VERSION}
export JRELEASER_BRANCH=${BRANCH}
export JRELEASER_CHOCOLATEY_GITHUB_BRANCH=${BRANCH}

jbang org.jreleaser:jreleaser:1.0.0 full-release \
  --git-root-search \
  -od target

popd
