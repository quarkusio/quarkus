#!/bin/bash

set -e -u -o pipefail
shopt -s failglob

if [ $# -eq 0 ]; then
    echo "Release version required"
    exit 1
fi
VERSION=$1

./mvnw -e -B -Dscan=false -Ddevelocity.cache.local.enabled=false -Ddevelocity.cache.remote.enabled=false versions:set -Dtcks -DnewVersion="${VERSION}" -DgenerateBackupPoms=false -DprocessAllModules -Prelocations -DupdateBuildOutputTimestampPolicy=always

if [ -f independent-projects/enforcer-rules/src/it/smoketest/pom.xml ]; then
    # update the parent version only using indentation
    sed -i -r "s@        <version>[^<]+</version>@        <version>${VERSION}</version>@" independent-projects/enforcer-rules/src/it/smoketest/pom.xml
fi

if [ -f devtools/gradle/gradle.properties ]; then
    sed -i -r "s/^version( ?= ?).*$/version\1${VERSION}/" devtools/gradle/gradle.properties
fi

if [ -f integration-tests/gradle/gradle.properties ]; then
    sed -i -r "s/^version( ?= ?).*$/version\1${VERSION}/" integration-tests/gradle/gradle.properties
fi

sed -r -i "s@<quarkus.version>[^<]+</quarkus.version>@<quarkus.version>${VERSION}</quarkus.version>@" independent-projects/tools/pom.xml

if [ -f extensions/azure-functions-http/maven-archetype/src/main/resources/archetype-resources/pom.xml ]; then
    sed -r -i "s@<quarkus.version>[^<]+</quarkus.version>@<quarkus.version>${VERSION}</quarkus.version>@" extensions/azure-functions-http/maven-archetype/src/main/resources/archetype-resources/pom.xml
    sed -r -i "s@<quarkus-plugin.version>[^<]+</quarkus-plugin.version>@<quarkus-plugin.version>${VERSION}</quarkus-plugin.version>@" extensions/azure-functions-http/maven-archetype/src/main/resources/archetype-resources/pom.xml
    sed -r -i "s@<quarkus.platform.version>[^<]+</quarkus.platform.version>@<quarkus.platform.version>${VERSION}</quarkus.platform.version>@" extensions/azure-functions-http/maven-archetype/src/main/resources/archetype-resources/pom.xml
fi

if [ -f extensions/amazon-lambda/maven-archetype/src/main/resources/archetype-resources/pom.xml ]; then
    sed -r -i "s@<quarkus.version>[^<]+</quarkus.version>@<quarkus.version>${VERSION}</quarkus.version>@" extensions/amazon-lambda/maven-archetype/src/main/resources/archetype-resources/pom.xml
    sed -r -i "s@<quarkus-plugin.version>[^<]+</quarkus-plugin.version>@<quarkus-plugin.version>${VERSION}</quarkus-plugin.version>@" extensions/amazon-lambda/maven-archetype/src/main/resources/archetype-resources/pom.xml
    sed -r -i "s@<quarkus.platform.version>[^<]+</quarkus.platform.version>@<quarkus.platform.version>${VERSION}</quarkus.platform.version>@" extensions/amazon-lambda/maven-archetype/src/main/resources/archetype-resources/pom.xml
fi

if [ -f extensions/amazon-lambda-http/maven-archetype/src/main/resources/archetype-resources/pom.xml ]; then
    sed -r -i "s@<quarkus.version>[^<]+</quarkus.version>@<quarkus.version>${VERSION}</quarkus.version>@" extensions/amazon-lambda-http/maven-archetype/src/main/resources/archetype-resources/pom.xml
    sed -r -i "s@<quarkus-plugin.version>[^<]+</quarkus-plugin.version>@<quarkus-plugin.version>${VERSION}</quarkus-plugin.version>@" extensions/amazon-lambda-http/maven-archetype/src/main/resources/archetype-resources/pom.xml
    sed -r -i "s@<quarkus.platform.version>[^<]+</quarkus.platform.version>@<quarkus.platform.version>${VERSION}</quarkus.platform.version>@" extensions/amazon-lambda-http/maven-archetype/src/main/resources/archetype-resources/pom.xml
fi

if [ -f extensions/funqy/funqy-amazon-lambda/maven-archetype/src/main/resources/archetype-resources/pom.xml ]; then
    sed -r -i "s@<quarkus.version>[^<]+</quarkus.version>@<quarkus.version>${VERSION}</quarkus.version>@" extensions/funqy/funqy-amazon-lambda/maven-archetype/src/main/resources/archetype-resources/pom.xml
    sed -r -i "s@<quarkus-plugin.version>[^<]+</quarkus-plugin.version>@<quarkus-plugin.version>${VERSION}</quarkus-plugin.version>@" extensions/funqy/funqy-amazon-lambda/maven-archetype/src/main/resources/archetype-resources/pom.xml
    sed -r -i "s@<quarkus.platform.version>[^<]+</quarkus.platform.version>@<quarkus.platform.version>${VERSION}</quarkus.platform.version>@" extensions/funqy/funqy-amazon-lambda/maven-archetype/src/main/resources/archetype-resources/pom.xml
fi
if [ -f extensions/amazon-lambda-rest/maven-archetype/src/main/resources/archetype-resources/pom.xml ]; then
    sed -r -i "s@<quarkus.version>[^<]+</quarkus.version>@<quarkus.version>${VERSION}</quarkus.version>@" extensions/amazon-lambda-rest/maven-archetype/src/main/resources/archetype-resources/pom.xml
    sed -r -i "s@<quarkus-plugin.version>[^<]+</quarkus-plugin.version>@<quarkus-plugin.version>${VERSION}</quarkus-plugin.version>@" extensions/amazon-lambda-rest/maven-archetype/src/main/resources/archetype-resources/pom.xml
    sed -r -i "s@<quarkus.platform.version>[^<]+</quarkus.platform.version>@<quarkus.platform.version>${VERSION}</quarkus.platform.version>@" extensions/amazon-lambda-rest/maven-archetype/src/main/resources/archetype-resources/pom.xml
fi
if [ -f coverage-report/pom.xml ]; then
    sed -r -i "s@^        <version>[^<]+</version>@        <version>${VERSION}</version>@" coverage-report/pom.xml
fi
