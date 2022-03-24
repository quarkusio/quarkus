#!/bin/bash

set -e -u -o pipefail
shopt -s failglob

# This script is used to gradually transform Quarkus bits from javax to jakarta namespaces and update dependencies

# Each transformed module/project is expected to:
# a) execute Eclipse Transformer command to transform relevant directory
# b) update dependencies to their respective EE 9 versions
# c) add a build and test command that will verify the functionality

if [ ! -f dco.txt ]; then
    echo "ERROR: This script has to be run from the root of the Quarkus project"
    exit 1
fi

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

# Set up jbang alias, we are using latest released transformer version
jbang alias add --name transform org.eclipse.transformer:org.eclipse.transformer.cli:0.2.0

start_module () {
  echo "# $1"
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

# Install root parent
./mvnw clean install -N

# Install utility projects
build_module_no_tests "independent-projects/ide-config"
build_module_no_tests "independent-projects/enforcer-rules"
build_module_no_tests "independent-projects/revapi"

# ArC
start_module "ArC"
transform_module "independent-projects/arc"
rewrite_module "independent-projects/arc"
build_module "independent-projects/arc"

# Bootstrap
start_module "Bootstrap"
rewrite_module "independent-projects/bootstrap/bom"
rewrite_module "independent-projects/bootstrap"
remove_banned_dependency "independent-projects/bootstrap" 'javax.inject:javax.inject' 'we allow javax.inject for Maven'
remove_banned_dependency "independent-projects/bootstrap" 'javax.annotation:javax.annotation-api' 'we allow javax.annotation-api for Maven'
build_module "independent-projects/bootstrap"

# Qute
build_module_no_tests "independent-projects/qute"

# Tools
remove_banned_dependency "independent-projects/tools" 'javax.inject:javax.inject' 'we allow javax.inject for Maven'
remove_banned_dependency "independent-projects/tools" 'javax.annotation:javax.annotation-api' 'we allow javax.annotation-api for Maven'
build_module_no_tests "independent-projects/tools"
rewrite_module "independent-projects/tools/devtools-common"
rewrite_module "independent-projects/tools"
build_module "independent-projects/tools"

## Starting here, we don't run the tests until post cleanup phase, use build_module_no_tests
## and then add another build of your module after the Clean up phase

# BOM
start_module "BOM"
rewrite_module "bom/application"
build_module_no_tests "bom/application"
build_module_only_no_tests "bom/test"

# Build parent
start_module "Build parent"
remove_banned_dependency "build-parent" 'javax.inject:javax.inject' 'we allow javax.inject for Maven'
remove_banned_dependency "build-parent" 'javax.annotation:javax.annotation-api' 'we allow javax.annotation-api for Maven'
update_banned_dependency "build-parent" 'jakarta.xml.bind:jakarta.xml.bind-api' 'org.jboss.spec.javax.xml.bind:jboss-jaxb-api_2.3_spec'
update_banned_dependency_advanced "build-parent" '<exclude>jakarta.ws.rs:jakarta.ws.rs-api</exclude>' "<exclude>jakarta.ws.rs:jakarta.ws.rs-api</exclude>\n                                            <exclude>org.jboss.spec.javax.ws.rs:jboss-jaxrs-api_2.1_spec</exclude>"
build_module_no_tests "build-parent"

# Needed for core
build_module_only_no_tests devtools
build_module_no_tests "devtools/platform-descriptor-json-plugin"
build_module_no_tests "devtools/platform-properties"

# Core
transform_module "core"
build_module_no_tests "core"
build_module_no_tests "core"

# Test framework
transform_module "test-framework"

## Clean up phase
## This removes the dependencies that shouldn't be there anymore - we need to do that once everything has been rewritten
rewrite_module_cleanup "bom/application"

# Build
build_module "bom/application"
build_module "bom/test"
build_module "build-parent"
build_module "core"
build_module_only_no_tests "test-framework"
build_module "test-framework/common"
build_module "test-framework/devmode-test-utils"
build_module "test-framework/junit5-properties"
build_module "test-framework/junit5"
build_module "test-framework/junit5-internal"
build_module "test-framework/maven"

# Extensions
transform_module "extensions"
build_module_only_no_tests "extensions"
build_module_only_no_tests "extensions/vertx-http"
build_module "extensions/vertx-http/dev-console-runtime-spi"
build_module "extensions/vertx-http/dev-console-spi"
# couldn't find a way to apply a rewrite here because we can't build the runtime module without the deployment module around :/
# I'm working on a different approach but let's live with it for now as I'm not sure my approach will fly and I want to unblock other people
sed -i 's@<groupId>org.jboss.spec.javax.ejb</groupId>@<groupId>jakarta.ejb</groupId>@' extensions/arc/deployment/pom.xml
sed -i 's@<artifactId>jboss-ejb-api_3.1_spec</artifactId>@<artifactId>jakarta.ejb-api</artifactId>@' extensions/arc/deployment/pom.xml
sed -i 's@<version>1.0.2.Final</version>@<version>4.0.0</version>@' extensions/arc/deployment/pom.xml
build_module "extensions/arc"

exit 1

# These ones require ArC and Munity extensions
#build_module "test-framework/junit5-mockito-config"
#build_module "test-framework/junit5-mockito"

# Dev Tools - needs to be done after all the extensions have been built and before we run the ITs
#transform_module "devtools"
#build_module_no_tests "devtools"

# For later, now that I moved it out of core
#rewrite_module "core/test-extension/runtime"

## Arc Extension [Incomplete: other modules need to go first]

# transform_module "extensions/arc/runtime"
# transform_module "extensions/arc/deployment"
# build_module "extensions/arc/runtime"
# build_module "extensions/arc/deployment"
