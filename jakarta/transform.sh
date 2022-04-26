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
  git clone -b jakarta --depth 1 https://github.com/gsmet/rewrite.git target/rewrite
  pushd target/rewrite
  ./gradlew -x test -x javadoc publishToMavenLocal
  popd

  rm -rf target/rewrite-maven-plugin
  git clone -b jakarta --depth 1 https://github.com/gsmet/rewrite-maven-plugin.git target/rewrite-maven-plugin
  pushd target/rewrite-maven-plugin
  ./mvnw -B clean install -DskipTests -DskipITs
  popd

  # Build SmallRye Config (temporary)
  #rm -rf target/smallrye-config
  #git clone https://github.com/smallrye/smallrye-config.git target/smallrye-config
  #pushd target/smallrye-config
  #git checkout jakarta
  #mvn clean install -DskipTests -DskipITs
  #popd

  # Build Agroal (temporary)
  rm -rf target/agroal
  git clone -b jakarta --depth 1 https://github.com/gsmet/agroal.git target/agroal
  pushd target/agroal
  mvn clean install -DskipTests -DskipITs
  popd

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

  # Build Kotlin Maven Plugin to allow skipping main compilation
  # (skipping test compilation is supported but not main)
  rm -rf target/kotlin
  git clone -b v1.6.21-jakarta --depth 1 https://github.com/gsmet/kotlin.git target/kotlin
  pushd target/kotlin/libraries/tools/kotlin-maven-plugin
  mvn -B clean install -DskipTests -DskipITs
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

transform_kotlin_module () {
  # this is very ad hoc but hopefully it will be good enough
  for package in javax.inject. javax.enterprise. javax.ws.rs. javax.annotation. javax.persistence. javax.json. javax.transaction.Transactional; do
    local newPackage=${package/javax/jakarta}
    find $1 -name '*.kt' | xargs sed -i "s@import ${package}@import ${newPackage}@g"
  done
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
./mvnw -B -pl :quarkus-platform-descriptor-json-plugin -pl :quarkus-bootstrap-maven-plugin -pl :quarkus-enforcer-rules -am clean install -DskipTests -DskipITs -Dinvoker.skip

## we cannot rewrite some of the modules for various reasons but we rewrite most of them
./mvnw -B -e rewrite:run -Denforcer.skip -Dprotoc.skip -Dmaven.main.skip -Dmaven.test.skip -Dforbiddenapis.skip -Dinvoker.skip -pl '!:quarkus-bom-quarkus-platform-descriptor' -pl '!:io.quarkus.gradle.plugin' -pl '!:io.quarkus.extension.gradle.plugin' -pl '!:quarkus-cli' -pl '!:quarkus-documentation' -pl '!:quarkus-integration-tests-parent' -Dno-test-modules -Drewrite.pomCacheEnabled=false -Djakarta-rewrite

## remove banned dependencies
remove_banned_dependency "independent-projects/bootstrap" 'javax.inject:javax.inject' 'we allow javax.inject for Maven'
remove_banned_dependency "independent-projects/bootstrap" 'javax.annotation:javax.annotation-api' 'we allow javax.annotation-api for Maven'
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

## RESTEasy Reactive
start_module "Tools"
transform_module "independent-projects/resteasy-reactive"
# TODO: probably something we need to push back to the Eclipse Transformer
convert_service_file independent-projects/resteasy-reactive/common/runtime/src/main/resources/META-INF/services/javax.ws.rs.ext.RuntimeDelegate
convert_service_file 'independent-projects/resteasy-reactive/client/runtime/src/main/resources/META-INF/services/javax.ws.rs.sse.SseEventSource$Builder'
convert_service_file independent-projects/resteasy-reactive/client/runtime/src/main/resources/META-INF/services/javax.ws.rs.client.ClientBuilder
build_module "independent-projects/resteasy-reactive"

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
build_module "extensions/smallrye-context-propagation"
build_module "extensions/mutiny"
build_module "extensions/netty"
build_module "extensions/vertx"
build_module "extensions/security"
build_module_only_no_tests "extensions/kubernetes"
build_module "extensions/kubernetes/spi"
build_module "extensions/vertx-http"
build_module "extensions/jsonp"
build_module "extensions/jaxrs-spi"
build_module "extensions/undertow"
build_module "extensions/jsonb"
build_module "extensions/jackson"
build_module "extensions/jaxp"
build_module "extensions/jaxb"
build_module "extensions/apache-httpclient"

# Some RESTEasy Reactive bits required for Hibernate Validator
# We will build RESTEasy Reactive properly later
build_module_only_no_tests "extensions/resteasy-reactive"
build_module_only_no_tests "extensions/resteasy-reactive/quarkus-resteasy-reactive-common"
build_module "extensions/resteasy-reactive/quarkus-resteasy-reactive-common/spi-deployment"

# Everything that is needed for RESTEasy Classic
build_module "extensions/elytron-security-common"
build_module "extensions/elytron-security"
# we need part of RESTEasy for elytron-security-properties-file, we will rebuild them properly later
build_module_only_no_tests "extensions/resteasy-classic"
build_module_no_tests "extensions/resteasy-classic/resteasy-common"
build_module_no_tests "extensions/resteasy-classic/resteasy-server-common"
build_module_no_tests "extensions/resteasy-classic/resteasy"
build_module_no_tests "extensions/resteasy-classic/resteasy-jsonb"
build_module "extensions/elytron-security-properties-file"
build_module "extensions/hibernate-validator"
build_module "extensions/reactive-routes"
build_module_only_no_tests "extensions/panache"
build_module "extensions/panache/panache-common"
build_module "extensions/qute"
build_module "extensions/smallrye-fault-tolerance"
convert_service_file "extensions/resteasy-classic/resteasy-multipart/runtime/src/main/resources/META-INF/services/javax.ws.rs.ext.Providers"
build_module "extensions/resteasy-classic"

# RESTEasy Reactive
build_module "extensions/smallrye-stork"
build_module "extensions/kotlin"
transform_kotlin_module "extensions/resteasy-reactive/quarkus-resteasy-reactive-kotlin"
transform_kotlin_module "extensions/resteasy-reactive/quarkus-resteasy-reactive-kotlin-serialization"
transform_kotlin_module "extensions/resteasy-reactive/quarkus-resteasy-reactive-kotlin-serialization-common"
transform_kotlin_module "extensions/resteasy-reactive/rest-client-reactive-kotlin-serialization"
update_scope_in_test_properties "extensions/resteasy-reactive/rest-client-reactive/deployment/src/test/resources/mp-configkey-scope-test-application.properties"
update_scope_in_test_properties "extensions/resteasy-reactive/rest-client-reactive/deployment/src/test/resources/mp-classname-scope-test-application.properties"
update_scope_in_test_properties "extensions/resteasy-reactive/rest-client-reactive/deployment/src/test/resources/mp-global-scope-test-application.properties"
build_module "extensions/resteasy-reactive"

# Lambda
build_module "extensions/amazon-lambda"
build_module "extensions/amazon-lambda-http"
build_module "extensions/amazon-lambda-rest"
build_module "extensions/amazon-lambda-xray"

# More SmallRye
build_module "extensions/smallrye-openapi-common"
build_module "extensions/swagger-ui"
build_module "extensions/smallrye-openapi"
build_module "extensions/smallrye-health"
build_module "extensions/smallrye-metrics"

# Persistence
build_module "extensions/credentials"
build_module "extensions/kubernetes-service-binding"
build_module_only_no_tests "extensions/datasource"
build_module_no_tests "extensions/datasource/common"
build_module_no_tests "extensions/datasource/deployment-spi/"
build_module "extensions/devservices"
build_module "extensions/datasource"
build_module "extensions/transaction-annotations"
build_module "extensions/narayana-jta"
build_module "test-framework/h2"
# we only build H2 as the others need Agroal for testing
build_module_only_no_tests "extensions/agroal"
build_module_only_no_tests "extensions/agroal/spi"
# this one needs particular care but we will rebuild it properly after
./mvnw clean install -f "extensions/agroal/runtime" -DskipExtensionValidation
build_module_only_no_tests "extensions/jdbc"
build_module "extensions/jdbc/jdbc-h2"
build_module "extensions/agroal"
build_module "extensions/jdbc"
build_module "extensions/caffeine"
build_module "extensions/panache/panache-hibernate-common"
build_module "extensions/hibernate-orm"
build_module "extensions/elasticsearch-rest-client-common"
build_module "extensions/elasticsearch-rest-client"
build_module "extensions/elasticsearch-rest-high-level-client"
build_module "extensions/hibernate-search-orm-elasticsearch"
build_module "extensions/avro"
build_module "extensions/hibernate-search-orm-coordination-outbox-polling"
build_module "extensions/reactive-datasource"
build_module "extensions/reactive-pg-client"
build_module "test-framework/vertx"
build_module "extensions/hibernate-reactive"

# And now in alphabetical order (dependency aside)...
# TODO apicurio-registry-avro depends on old JAX-RS spec
build_module "extensions/awt"
build_module "extensions/azure-functions-http"
build_module "extensions/cache"
build_module "extensions/config-yaml"
build_module "extensions/kubernetes-client"
build_module "extensions/container-image"
build_module "extensions/elytron-security-jdbc"
build_module "test-framework/ldap"
build_module "extensions/elytron-security-ldap"
build_module "extensions/elytron-security-oauth2"
build_module "extensions/flyway"
build_module "extensions/funqy"
build_module "extensions/google-cloud-functions"
build_module "extensions/google-cloud-functions-http"
build_module "extensions/grpc-common"
build_module "extensions/grpc"
build_module "extensions/hibernate-envers"
build_module "extensions/infinispan-client"
build_module "extensions/jaeger"
build_module "extensions/kafka-client"
build_module "test-framework/junit5-mockito-config"
build_module "test-framework/junit5-mockito"
build_module "extensions/kafka-streams"
build_module "test-framework/keycloak-server"
build_module "extensions/keycloak-admin-client"
build_module "extensions/keycloak-admin-client-reactive"
build_module "extensions/smallrye-jwt-build"
build_module "extensions/oidc-common"
build_module "extensions/oidc"
build_module "extensions/keycloak-authorization"
build_module "extensions/kubernetes"
build_module "extensions/kubernetes-config"
build_module "extensions/liquibase"
build_module "extensions/reactive-streams-operators"
build_module "extensions/smallrye-opentracing"
build_module "extensions/mongodb-client"
build_module "extensions/liquibase-mongodb"
build_module "extensions/logging-gelf"
build_module "extensions/logging-json"
build_module "extensions/mailer"
build_module "extensions/micrometer"
build_module "extensions/micrometer-registry-prometheus"
# TODO we need a narayana-lra Jakarta extension (dependency of MP spec and CDI spec)
#build_module "extensions/narayana-lra"
build_module "extensions/narayana-stm"
build_module "extensions/oidc-client"
build_module "extensions/oidc-client-filter"
build_module "extensions/oidc-client-reactive-filter"
build_module "extensions/oidc-token-propagation"
build_module "extensions/oidc-token-propagation-reactive"
build_module "extensions/openshift-client"
build_module "extensions/opentelemetry"
build_module "extensions/reactive-datasource"
transform_kotlin_module "extensions/panache/hibernate-orm-panache-kotlin"
transform_kotlin_module "extensions/panache/mongodb-panache-kotlin"
sed -i "s@javax/xml/bind/annotation/@jakarta/xml/bind/annotation/@g" ./extensions/panache/panache-common/deployment/src/main/java/io/quarkus/panache/common/deployment/PanacheConstants.java
build_module "extensions/panache"
build_module "extensions/picocli"
transform_kotlin_module "extensions/scheduler/kotlin"
build_module "extensions/scheduler"
build_module "extensions/quartz"
build_module "extensions/reactive-db2-client"
build_module "extensions/reactive-mssql-client"
build_module "extensions/reactive-mysql-client"
build_module "extensions/reactive-oracle-client"
build_module "extensions/reactive-pg-client"
build_module "extensions/redis-client"
build_module "extensions/scala"
build_module "extensions/security-jpa"
build_module_only_no_tests "extensions/security-webauthn"
./mvnw clean install -f "extensions/security-webauthn/runtime" -DskipExtensionValidation
build_module "test-framework/security-webauthn"
build_module "extensions/security-webauthn"
# TODO we need a Jakarta version of SmallRye GraphQL
#build_module "extensions/smallrye-graphql"
#build_module "extensions/smallrye-graphql-client"
build_module "extensions/smallrye-jwt"
transform_kotlin_module "extensions/smallrye-reactive-messaging/kotlin"
# TODO we need a Jakarta version for SmallRye Reactive Messaging
#build_module "extensions/smallrye-reactive-messaging"
#build_module "extensions/smallrye-reactive-messaging-amqp"
#build_module "extensions/smallrye-reactive-messaging-kafka"
#build_module "extensions/smallrye-reactive-messaging-mqtt"
#build_module "extensions/smallrye-reactive-messaging-rabbitmq"
build_module "extensions/spring-boot-properties"
build_module "extensions/spring-cache"
build_module "extensions/spring-cloud-config-client"
build_module "extensions/spring-di"
build_module "extensions/spring-data-jpa"
build_module "extensions/spring-data-rest"
build_module "extensions/spring-scheduled"
# TODO new version of RESTEasy Spring Web has been released with JDK 17
build_module "extensions/spring-web"
build_module "extensions/spring-security"
build_module "extensions/vertx-graphql"
build_module "extensions/webjars-locator"
build_module "extensions/websockets"

exit 0

# These ones require ArC and Mutiny extensions
#build_module "test-framework/junit5-mockito-config"
#build_module "test-framework/junit5-mockito"


# Dev Tools - needs to be done after all the extensions have been built and before we run the ITs
#transform_module "devtools"
#build_module_no_tests "devtools"

