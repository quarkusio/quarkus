#!/bin/bash

set -e -u -o pipefail
shopt -s failglob

# Each transformed module/project is expected to:
# a) execute Eclipse Transformer command to transform relevant directory
# b) update dependencies to their respective EE 9 versions
# c) add a build and test command that will verify the functionality

if [ ! -f dco.txt ]; then
    echo "ERROR: This script has to be run from the root of the Quarkus project"
    exit 1
fi

if [ "${REWRITE_OFFLINE-false}" != "true" ]; then
  # Prepare OpenRewrite - we temporarily build a local version as we need a patch
  rm -rf target/rewrite
  git clone git@github.com:gsmet/rewrite.git target/rewrite
  pushd target/rewrite
  git checkout jakarta
  ./gradlew -x test -x javadoc publishToMavenLocal
  popd

  rm -rf target/rewrite-maven-plugin
  git clone git@github.com:gsmet/rewrite-maven-plugin.git target/rewrite-maven-plugin
  pushd target/rewrite-maven-plugin
  git checkout jakarta
  ./mvnw clean install -DskipTests -DskipITs
  popd

  # Build SmallRye Config (temporary)
  rm -rf target/smallrye-config
  git clone git@github.com:smallrye/smallrye-config.git target/smallrye-config
  pushd target/smallrye-config
  git checkout jakarta
  mvn clean install -DskipTests -DskipITs
  popd

  # Build Kotlin Maven Plugin to allow skipping main compilation
  # (skipping test compilation is supported but not main)
  rm -rf target/kotlin
  git clone -b v1.6.10-jakarta --depth 1 git@github.com:gsmet/kotlin.git target/kotlin
  pushd target/kotlin/libraries/tools/kotlin-maven-plugin
  mvn clean install -DskipTests -DskipITs
  popd
fi

# Set up jbang alias, we are using latest released transformer version
jbang alias add --name transform org.eclipse.transformer:org.eclipse.transformer.cli:0.2.0

start_module () {
  echo "# $1"
}

clean_project () {
  find . -name 'target' -exec rm -rf {} +
}

clean_maven_repository () {
  if [ -d ~/.m2/repository/io/quarkus ]; then
    find ~/.m2/repository/io/quarkus -name '999-SNAPSHOT' -exec rm -rf {} +
  fi
}

# Function to help transform a particular Maven module using Eclipse Transformer
transform_module () {
  local modulePath="$1"
  local transformationTemp="JAKARTA_TEMP"
  rm -Rf $transformationTemp
  mkdir $transformationTemp
  echo "  - Transforming $modulePath"
  jbang transform -o $modulePath $transformationTemp
  rm -Rf "$modulePath"
  mv "$transformationTemp" "$modulePath"
  echo "    > Transformation done"
}

# Rewrite a module with OpenRewrite
rewrite_module () {
  local modulePath="$1"
  echo "  - Rewriting $modulePath"
  ./mvnw -B rewrite:run -f "${modulePath}/pom.xml" -N
  echo "    > Rewriting done"
}

# Rewrite a module with OpenRewrite but with the rewrite-cleanup profile
rewrite_module_cleanup () {
  local modulePath="$1"
  echo "  - Rewriting $modulePath"
  ./mvnw -B rewrite:run -f "${modulePath}/pom.xml" -N -Prewrite-cleanup
  echo "    > Rewriting done"
}

# Remove a banned dependency
remove_banned_dependency () {
  sed -i "s@<exclude>$2</exclude>@<!-- $3 -->@g" $1/pom.xml
}

# Update a banned dependency
update_banned_dependency () {
  sed -i "s@<exclude>$2</exclude>@<exclude>$3</exclude>@g" $1/pom.xml
}

update_banned_dependency_advanced () {
  sed -i "s@$2@$3@g" $1/pom.xml
}

# Build, test and install a particular maven module (chosen by relative path)
build_module () {
  local pomPath="$1/pom.xml"
  ./mvnw -B clean install -f "$pomPath"
  echo "  - Installed newly built $pomPath"
}

# Build module without testing it
build_module_no_tests () {
  local pomPath="$1/pom.xml"
  ./mvnw -B clean install -f "$pomPath" -DskipTests -DskipITs
  echo "  - Installed newly built $pomPath"
}

build_module_only_no_tests () {
  local pomPath="$1/pom.xml"
  ./mvnw -B clean install -f "$pomPath" -DskipTests -DskipITs -N
  echo "  - Installed newly built $pomPath"
}

# Sets the EDITING variable to the file being edited by set_property
edit_begin () {
  EDITING="$1"
}

# Finds a particular property and replaces its value
set_property () {
  if [ "$#" -ne 2 ]; then
      echo "Requires two parameters"
  fi
  local propName=$1
  local propValue=$2
  sed -i "s/<$propName>.*<\/$propName>/<$propName>$propValue<\/$propName>/g" "$EDITING"
}

###############################
###############################

# OpenRewrite phase - we rewrite the whole repository in one go
clean_maven_repository
clean_project

## let's build what's required to be able to run the rewrite
./mvnw -pl :quarkus-platform-descriptor-json-plugin -pl :quarkus-bootstrap-maven-plugin -pl :quarkus-enforcer-rules -am clean install -DskipTests -DskipITs

## we cannot rewrite some of the modules for various reasons but we rewrite most of them
./mvnw rewrite:run -Denforcer.skip -Dprotoc.skip -Dmaven.main.skip -Dmaven.test.skip -Dforbiddenapis.skip -pl '!:quarkus-bom-quarkus-platform-descriptor' -pl '!:io.quarkus.gradle.plugin' -pl '!:io.quarkus.extension.gradle.plugin' -pl '!:quarkus-cli' -pl '!:quarkus-documentation' -Dno-test-modules -Drewrite.pomCacheEnabled=false

## remove banned dependencies
remove_banned_dependency "independent-projects/bootstrap" 'javax.inject:javax.inject' 'we allow javax.inject for Maven'
remove_banned_dependency "independent-projects/bootstrap" 'javax.annotation:javax.annotation-api' 'we allow javax.annotation-api for Maven'
remove_banned_dependency "independent-projects/tools" 'javax.inject:javax.inject' 'we allow javax.inject for Maven'
remove_banned_dependency "independent-projects/tools" 'javax.annotation:javax.annotation-api' 'we allow javax.annotation-api for Maven'
remove_banned_dependency "build-parent" 'javax.inject:javax.inject' 'we allow javax.inject for Maven'
remove_banned_dependency "build-parent" 'javax.annotation:javax.annotation-api' 'we allow javax.annotation-api for Maven'
update_banned_dependency "build-parent" 'jakarta.xml.bind:jakarta.xml.bind-api' 'org.jboss.spec.javax.xml.bind:jboss-jaxb-api_2.3_spec'
update_banned_dependency_advanced "build-parent" '<exclude>jakarta.ws.rs:jakarta.ws.rs-api</exclude>' "<exclude>jakarta.ws.rs:jakarta.ws.rs-api</exclude>\n                                            <exclude>org.jboss.spec.javax.ws.rs:jboss-jaxrs-api_2.1_spec</exclude>"

## cleanup phase - needs to be done once everything has been rewritten
rewrite_module_cleanup "bom/application"

## we get rid of everything that has been built previously, we want to start clean
clean_maven_repository

# Tranformation phase

## Install root parent
./mvnw clean install -N

## Install utility projects
build_module_no_tests "independent-projects/ide-config"
build_module_no_tests "independent-projects/enforcer-rules"
build_module_no_tests "independent-projects/revapi"

## ArC
start_module "ArC"
transform_module "independent-projects/arc"
build_module "independent-projects/arc"

## Bootstrap
start_module "Bootstrap"
build_module "independent-projects/bootstrap"

## Qute
start_module "Qute"
build_module_no_tests "independent-projects/qute"

## Tools
start_module "Tools"
build_module "independent-projects/tools"

# BOM
start_module "BOM"
build_module "bom/application"
build_module "bom/test"

# Build parent
start_module "Build parent"
build_module "build-parent"

# Core
start_module "Core"

## Needed for core
build_module_only_no_tests devtools
build_module_no_tests "devtools/platform-descriptor-json-plugin"
build_module_no_tests "devtools/platform-properties"

## Core
transform_module "core"
build_module_no_tests "core"

# Test framework
start_module "Test framework"
transform_module "test-framework"
build_module_only_no_tests "test-framework"
build_module "test-framework/common"
build_module "test-framework/devmode-test-utils"
build_module "test-framework/junit5-properties"
build_module "test-framework/junit5"
build_module "test-framework/junit5-internal"
build_module "test-framework/maven"

# Extensions
start_module "Extensions"
transform_module "extensions"
build_module_only_no_tests "extensions"
build_module_only_no_tests "extensions/vertx-http"
build_module "extensions/vertx-http/dev-console-runtime-spi"
build_module "extensions/vertx-http/dev-console-spi"
build_module "extensions/arc"

exit 1

# These ones require ArC and Mutiny extensions
#build_module "test-framework/junit5-mockito-config"
#build_module "test-framework/junit5-mockito"

# Dev Tools - needs to be done after all the extensions have been built and before we run the ITs
#transform_module "devtools"
#build_module_no_tests "devtools"

