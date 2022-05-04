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

if [ "${REWRITE_OFFLINE-false}" != "true" ]; then
  # Build SmallRye Config (temporary)
  #rm -rf target/smallrye-config
  #git clone https://github.com/smallrye/smallrye-config.git target/smallrye-config
  #pushd target/smallrye-config
  #git checkout jakarta
  #mvn clean install -DskipTests -DskipITs
  #popd

  # Build Quarkus HTTP (temporary)
  rm -rf target/quarkus-http
  git clone -b jakarta-rewrite --depth 1 https://github.com/quarkusio/quarkus-http.git target/quarkus-http
  pushd target/quarkus-http
  mvn -B clean install -DskipTests -DskipITs
  popd

  # Build Quarkus Security (temporary)
  rm -rf target/quarkus-security
  git clone -b jakarta-rewrite --depth 1 https://github.com/quarkusio/quarkus-security.git target/quarkus-security
  pushd target/quarkus-security
  mvn -B clean install -DskipTests -DskipITs
  popd

  # Build Keycloak (temporary)
  rm -rf target/keycloak
  git clone -b jakarta --depth 1 https://github.com/gsmet/keycloak.git target/keycloak
  pushd target/keycloak
  mvn -B -pl ':keycloak-admin-client-jakarta' -am clean install -DskipTests -DskipITs
  popd

  # Build Narayana (temporary)
  rm -rf target/narayana
  git clone -b JBTM-3595-martin --depth 1 https://github.com/mmusgrov/narayana.git target/narayana
  pushd target/narayana
  ./build.sh clean install -DskipTests -DskipITs -Pcommunity
  popd
fi
