#!/bin/bash

set -e -u -o pipefail
shopt -s failglob

# Use `export REWRITE_OFFLINE=true` to avoid building components from external repositories
# Use `export REWRITE_NO_TESTS=true` to avoid running the tests (useful when you just want a quick transformation)

# Each transformed module/project is expected to:
# a) execute Eclipse Transformer command to transform relevant directory
# b) update dependencies to their respective EE 9 versions
# c) add a build and test command that will verify the functionality

if [ ! -f dco.txt ]; then
    echo "ERROR: This script has to be run from the root of the Quarkus project"
    exit 1
fi

#if [ "${REWRITE_OFFLINE-false}" != "true" ]; then
  # Build SmallRye Config (temporary)
  #rm -rf target/smallrye-config
  #git clone https://github.com/smallrye/smallrye-config.git target/smallrye-config
  #pushd target/smallrye-config
  #git checkout jakarta
  #mvn clean install -DskipTests -DskipITs
  #popd

  # Build Narayana (temporary)
  #rm -rf target/narayana
  #git clone -b JBTM-3595-martin --depth 1 https://github.com/mmusgrov/narayana.git target/narayana
  #pushd target/narayana
  #./build.sh clean install -DskipTests -DskipITs -Pcommunity
  #popd
#fi
