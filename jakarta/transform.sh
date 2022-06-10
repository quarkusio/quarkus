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
  # Prepare OpenRewrite - we temporarily build a local version as we need a patch
  rm -rf target/rewrite
  git clone https://github.com/gsmet/rewrite.git target/rewrite
  pushd target/rewrite
  git checkout jakarta
  ./gradlew -x test -x javadoc publishToMavenLocal
  popd

  rm -rf target/rewrite-maven-plugin
  git clone -b jakarta --depth 1 https://github.com/gsmet/rewrite-maven-plugin.git target/rewrite-maven-plugin
  pushd target/rewrite-maven-plugin
  ./mvnw -B clean install -DskipTests -DskipITs
  popd

  # Build Kotlin Maven Plugin to allow skipping main compilation
  # (skipping test compilation is supported but not main)
  rm -rf target/kotlin
  git clone -b v1.6.21-jakarta --depth 1 https://github.com/gsmet/kotlin.git target/kotlin
  pushd target/kotlin/libraries/tools/kotlin-maven-plugin
  mvn -B clean install -DskipTests -DskipITs
  popd
fi

./jakarta/prepare.sh

# Set up jbang alias, we are using latest released transformer version
jbang alias add --name transform org.eclipse.transformer:org.eclipse.transformer.cli:0.4.0

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

transform_kotlin_module () {
  # this is very ad hoc but hopefully it will be good enough
  for package in javax.inject. javax.enterprise. javax.ws.rs. javax.annotation. javax.persistence. javax.json. javax.websocket. javax.xml.bind. javax.transaction.Transactional; do
    local newPackage=${package/javax/jakarta}
    find $1 -name '*.kt' | xargs --no-run-if-empty sed -i "s@import ${package}@import ${newPackage}@g"
  done
}

transform_java_module () {
  # this is very ad hoc but hopefully it will be good enough
  for package in javax.inject. javax.enterprise. javax.ws.rs. javax.annotation. javax.persistence. javax.json. javax.websocket. javax.xml.bind. javax.transaction.Transactional; do
    local newPackage=${package/javax/jakarta}
    find $1 -name '*.java' | xargs --no-run-if-empty sed -i "s@import ${package}@import ${newPackage}@g"
  done
}

transform_scala_module () {
  # this is very ad hoc but hopefully it will be good enough
  for package in javax.inject. javax.enterprise. javax.ws.rs. javax.annotation. javax.persistence. javax.json. javax.websocket. javax.xml.bind. javax.transaction.Transactional; do
    local newPackage=${package/javax/jakarta}
    find $1 -name '*.scala' | xargs --no-run-if-empty sed -i "s@import ${package}@import ${newPackage}@g"
  done
}

transform_documentation () {
  local transformationTemp="JAKARTA_TEMP"
  rm -Rf $transformationTemp
  mkdir $transformationTemp
  jbang transform -tf jakarta/jakarta-text-adoc.properties -o docs/ $transformationTemp
  rm -Rf "docs/"
  mv "$transformationTemp" "docs"
}

update_scope_in_test_properties () {
  sed -i "s@javax.enterprise@jakarta.enterprise@g" $1
}

convert_service_file () {
  local newName=${1/javax/jakarta}
  mv $1 $newName
}

# Rewrite a module with OpenRewrite
rewrite_module () {
  local modulePath="$1"
  echo "  - Rewriting $modulePath"
  ./mvnw -B rewrite:run -f "${modulePath}/pom.xml" -N -Djakarta-rewrite
  echo "    > Rewriting done"
}

# Rewrite a module with OpenRewrite but with the rewrite-cleanup profile
rewrite_module_cleanup () {
  local modulePath="$1"
  echo "  - Rewriting $modulePath"
  ./mvnw -B rewrite:run -f "${modulePath}/pom.xml" -N -Djakarta-rewrite-cleanup
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
  if [ "${REWRITE_TESTS_CONTAINERS-false}" == "true" ]; then
    ./mvnw -B clean install -f "$pomPath" -Dstart-containers -Dtest-containers
  elif [ "${REWRITE_NO_TESTS-false}" != "true" ]; then
    ./mvnw -B clean install -f "$pomPath"
  else
    ./mvnw -B clean install -f "$pomPath" -DskipTests -DskipITs
  fi
  echo "  - Installed newly built $pomPath"
}

# Build module without testing it
build_module_no_tests () {
  local pomPath="$1/pom.xml"
  ./mvnw -B clean install -f "$pomPath" -DskipTests -DskipITs -Dinvoker.skip
  echo "  - Installed newly built $pomPath"
}

build_module_only_no_tests () {
  local pomPath="$1/pom.xml"
  ./mvnw -B clean install -f "$pomPath" -DskipTests -DskipITs -Dinvoker.skip -N
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
./mvnw -B -pl :quarkus-platform-descriptor-json-plugin -pl :quarkus-bootstrap-maven-plugin -pl :quarkus-enforcer-rules -pl :quarkus-maven-plugin -pl :quarkus-bom-test -am clean install -DskipTests -DskipITs -Dinvoker.skip

## we cannot rewrite some of the modules for various reasons but we rewrite most of them
./mvnw -B -e rewrite:run -Denforcer.skip -Dprotoc.skip -Dmaven.main.skip -Dmaven.test.skip -Dforbiddenapis.skip -Dinvoker.skip -Dquarkus.generate-code.skip -Dquarkus.build.skip -DskipExtensionValidation -DskipCodestartValidation -Pbom-descriptor-json-hollow -pl '!:io.quarkus.gradle.plugin' -pl '!:io.quarkus.extension.gradle.plugin' -pl '!:quarkus-integration-test-gradle-plugin' -pl '!:quarkus-documentation' -Drewrite.pomCacheEnabled=false -Djakarta-rewrite

## remove banned dependencies
remove_banned_dependency "independent-projects/bootstrap" 'javax.inject:javax.inject' 'we allow javax.inject for Maven'
remove_banned_dependency "independent-projects/bootstrap" 'javax.annotation:javax.annotation-api' 'we allow javax.annotation-api for Maven'
remove_banned_dependency "independent-projects/extension-maven-plugin" 'javax.inject:javax.inject' 'we allow javax.inject for Maven'
remove_banned_dependency "independent-projects/extension-maven-plugin" 'javax.annotation:javax.annotation-api' 'we allow javax.annotation-api for Maven'
remove_banned_dependency "independent-projects/tools" 'javax.inject:javax.inject' 'we allow javax.inject for Maven'
remove_banned_dependency "independent-projects/tools" 'javax.annotation:javax.annotation-api' 'we allow javax.annotation-api for Maven'
update_banned_dependency "independent-projects/resteasy-reactive" 'jakarta.xml.bind:jakarta.xml.bind-api' 'org.jboss.spec.javax.xml.bind:jboss-jaxb-api_2.3_spec'
remove_banned_dependency "build-parent" 'javax.inject:javax.inject' 'we allow javax.inject for Maven'
remove_banned_dependency "build-parent" 'javax.annotation:javax.annotation-api' 'we allow javax.annotation-api for Maven'
update_banned_dependency "build-parent" 'jakarta.xml.bind:jakarta.xml.bind-api' 'org.jboss.spec.javax.xml.bind:jboss-jaxb-api_2.3_spec'
# TODO: due to an issue in the MicroProfile REST Client, we cannot exclude jakarta.ws.rs:jakarta.ws.rs-api yet
#update_banned_dependency_advanced "build-parent" '<exclude>jakarta.ws.rs:jakarta.ws.rs-api</exclude>' "<exclude>jakarta.ws.rs:jakarta.ws.rs-api</exclude>\n                                            <exclude>org.jboss.spec.javax.ws.rs:jboss-jaxrs-api_2.1_spec</exclude>"
update_banned_dependency_advanced "build-parent" '<exclude>jakarta.ws.rs:jakarta.ws.rs-api</exclude>' "<!-- exclude>jakarta.ws.rs:jakarta.ws.rs-api</exclude -->\n                                            <exclude>org.jboss.spec.javax.ws.rs:jboss-jaxrs-api_2.1_spec</exclude>"
update_banned_dependency_advanced "build-parent" '<exclude>jakarta.json:jakarta.json-api</exclude>' "<exclude>jakarta.json:jakarta.json-api</exclude>\n                                            <exclude>org.glassfish:jakarta.json</exclude>"

## some additional wild changes to clean up at some point
sed -i 's@FilterConfigSourceImpl@FilterConfigSource@g' extensions/resteasy-classic/resteasy-common/deployment/src/main/java/io/quarkus/resteasy/common/deployment/ResteasyCommonProcessor.java
sed -i 's@ServletConfigSourceImpl@ServletConfigSource@g' extensions/resteasy-classic/resteasy-common/deployment/src/main/java/io/quarkus/resteasy/common/deployment/ResteasyCommonProcessor.java
sed -i 's@ServletContextConfigSourceImpl@ServletContextConfigSource@g' extensions/resteasy-classic/resteasy-common/deployment/src/main/java/io/quarkus/resteasy/common/deployment/ResteasyCommonProcessor.java
# Unfortunately, this file has been copied from RESTEasy with some adjustments and it is not compatible with the new RESTEasy MicroProfile
# I started some discussions with James Perkins about how to contribute back our changes
# For now, this will have to do
cp "jakarta/overrides/rest-client/QuarkusRestClientBuilder.java" "extensions/resteasy-classic/rest-client/runtime/src/main/java/io/quarkus/restclient/runtime/"
cp "jakarta/overrides/jaxb/JAXBSubstitutions.java" "extensions/jaxb/runtime/src/main/java/io/quarkus/jaxb/runtime/graal/"

## JSON-P implementation switch
sed -i 's@<runnerParentFirstArtifact>org.glassfish:jakarta.json</runnerParentFirstArtifact>@<runnerParentFirstArtifact>org.eclipse.parsson:jakarta.json</runnerParentFirstArtifact>@g' extensions/logging-json/runtime/pom.xml
sed -i 's@<parentFirstArtifact>org.glassfish:jakarta.json</parentFirstArtifact>@<parentFirstArtifact>org.eclipse.parsson:jakarta.json</parentFirstArtifact>@g' extensions/jsonp/runtime/pom.xml
sed -i 's@import org.glassfish.json.JsonProviderImpl;@import org.eclipse.parsson.JsonProviderImpl;@g' extensions/jsonp/deployment/src/main/java/io/quarkus/jsonp/deployment/JsonpProcessor.java

## cleanup phase - needs to be done once everything has been rewritten
rewrite_module_cleanup "bom/application"
rewrite_module_cleanup "independent-projects/resteasy-reactive"

## we get rid of everything that has been built previously, we want to start clean
clean_maven_repository

# Tranformation phase

transform_module "independent-projects/arc"
transform_module "independent-projects/resteasy-reactive"
transform_module "independent-projects/tools"
transform_kotlin_module "independent-projects/tools/base-codestarts/src/main/resources/codestarts"
transform_java_module "independent-projects/tools/base-codestarts/src/main/resources/codestarts"
transform_scala_module "independent-projects/tools/base-codestarts/src/main/resources/codestarts"
transform_kotlin_module "independent-projects/tools/base-codestarts/src/main/resources/codestarts"
transform_java_module "independent-projects/tools/base-codestarts/src/main/resources/codestarts"
transform_scala_module "independent-projects/tools/base-codestarts/src/main/resources/codestarts"
transform_kotlin_module "independent-projects/tools/devtools-testing/"
transform_java_module "independent-projects/tools/devtools-testing/"
transform_scala_module "independent-projects/tools/devtools-testing/"
transform_kotlin_module "independent-projects/tools/codestarts"
transform_java_module "independent-projects/tools/codestarts"
transform_scala_module "independent-projects/tools/codestarts"
#convert_service_file independent-projects/resteasy-reactive/common/runtime/src/main/resources/META-INF/services/javax.ws.rs.ext.RuntimeDelegate
#convert_service_file 'independent-projects/resteasy-reactive/client/runtime/src/main/resources/META-INF/services/javax.ws.rs.sse.SseEventSource$Builder'
#convert_service_file independent-projects/resteasy-reactive/client/runtime/src/main/resources/META-INF/services/javax.ws.rs.client.ClientBuilder
transform_module "core"
transform_module "test-framework"
transform_module "extensions"
#convert_service_file "extensions/resteasy-classic/resteasy-multipart/runtime/src/main/resources/META-INF/services/javax.ws.rs.ext.Providers"
transform_kotlin_module "extensions/resteasy-reactive/quarkus-resteasy-reactive-kotlin"
transform_kotlin_module "extensions/resteasy-reactive/quarkus-resteasy-reactive-kotlin-serialization"
transform_kotlin_module "extensions/resteasy-reactive/quarkus-resteasy-reactive-kotlin-serialization-common"
transform_kotlin_module "extensions/resteasy-reactive/rest-client-reactive-kotlin-serialization"
update_scope_in_test_properties "extensions/resteasy-reactive/rest-client-reactive/deployment/src/test/resources/mp-configkey-scope-test-application.properties"
update_scope_in_test_properties "extensions/resteasy-reactive/rest-client-reactive/deployment/src/test/resources/mp-classname-scope-test-application.properties"
update_scope_in_test_properties "extensions/resteasy-reactive/rest-client-reactive/deployment/src/test/resources/mp-global-scope-test-application.properties"
transform_kotlin_module "extensions/panache/hibernate-orm-panache-kotlin"
transform_kotlin_module "extensions/panache/mongodb-panache-kotlin"
sed -i "s@javax/xml/bind/annotation/@jakarta/xml/bind/annotation/@g" ./extensions/panache/panache-common/deployment/src/main/java/io/quarkus/panache/common/deployment/PanacheConstants.java
transform_kotlin_module "extensions/scheduler/kotlin"
transform_kotlin_module "extensions/smallrye-reactive-messaging/kotlin"
transform_module "devtools"
transform_kotlin_module "devtools/project-core-extension-codestarts/src/main/resources/codestarts/"
transform_java_module "devtools/project-core-extension-codestarts/src/main/resources/codestarts/"
transform_scala_module "devtools/project-core-extension-codestarts/src/main/resources/codestarts/"
transform_module "integration-tests"
transform_scala_module "integration-tests/scala"
transform_kotlin_module "integration-tests"

sed -i 's/@javax.annotation.Generated/@jakarta.annotation.Generated/g' extensions/grpc/protoc/src/main/resources/*.mustache

transform_documentation
sed -i 's@javax/ws/rs@jakarta/ws/rs@g' docs/src/main/asciidoc/resteasy-reactive.adoc
sed -i 's@https://javadoc.io/doc/jakarta.ws.rs/jakarta.ws.rs-api/2.1.1@https://javadoc.io/doc/jakarta.ws.rs/jakarta.ws.rs-api/3.1.0@g' docs/src/main/asciidoc/resteasy-reactive.adoc
sed -i 's@/specs/jaxrs/2.1/index.html@https://jakarta.ee/specifications/restful-ws/3.1/jakarta-restful-ws-spec-3.1.html@g' docs/src/main/asciidoc/resteasy-reactive.adoc

# Clean some Transformer issues
git checkout -- devtools/gradle/
git checkout -- integration-tests/gradle/gradle/wrapper/gradle-wrapper.jar
git checkout -- independent-projects/tools/base-codestarts/src/main/resources/codestarts/quarkus/tooling/gradle-wrapper/base/gradle/wrapper/gradle-wrapper.jar
git checkout -- extensions/kubernetes-service-binding/runtime/src/test/resources/k8s/test-k8s/

# Build phase

if [ "${REWRITE_TESTS_CONTAINERS-false}" == "true" ]; then
  ./mvnw -B clean install -Dno-test-modules -Dstart-containers -Dtest-containers
elif [ "${REWRITE_NO_TESTS-false}" != "true" ]; then
  ./mvnw -B clean install -Dno-test-modules
else
  ./mvnw -B clean install -Dno-test-modules -DskipTests -DskipITs
fi

# - Infinispan uses the @javax.annotation.Generated annotation in code generation and it's not available
# - Confluent registry client doesn't have a version supporting Jakarta packages
# - gRPC protoc plugin generates classes with @javax.annotation.Generated
./mvnw -B clean install -f integration-tests -DskipTests -DskipITs -pl '!:quarkus-integration-test-infinispan-client' -pl '!:quarkus-integration-test-kafka-avro' -pl '!:quarkus-integration-test-opentelemetry-grpc' -pl '!:quarkus-integration-test-grpc-tls' -pl '!:quarkus-integration-test-grpc-plain-text-gzip' -pl '!:quarkus-integration-test-grpc-plain-text-mutiny' -pl '!:quarkus-integration-test-grpc-mutual-auth' -pl '!:quarkus-integration-test-grpc-streaming' -pl '!:quarkus-integration-test-grpc-interceptors' -pl '!:quarkus-integration-test-grpc-proto-v2' -pl '!:quarkus-integration-test-grpc-hibernate' -pl '!:quarkus-integration-test-grpc-hibernate-reactive' -pl '!:quarkus-integration-test-grpc-external-proto-test' -pl '!:quarkus-integration-test-grpc-stork-response-time'

exit 0

