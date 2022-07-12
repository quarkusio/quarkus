#!/bin/bash

set -e -u -o pipefail
shopt -s failglob

if [ $# -eq 0 ]; then
    echo "Release version required"
    exit 1
fi
VERSION=$1

./mvnw versions:set -DnewVersion="${VERSION}" -DgenerateBackupPoms=false -DprocessAllModules -Prelocations

if [ -f devtools/gradle/gradle.properties ]; then
    sed -i -r "s/^version( ?= ?).*$/version\1${VERSION}/" devtools/gradle/gradle.properties
fi

if [ -f integration-tests/gradle/gradle.properties ]; then
    sed -i -r "s/^version( ?= ?).*$/version\1${VERSION}/" integration-tests/gradle/gradle.properties
fi

sed -i "s@<quarkus.version>999-SNAPSHOT</quarkus.version>@<quarkus.version>${VERSION}</quarkus.version>@" independent-projects/tools/pom.xml

sed -i "s@<quarkus.version>999-SNAPSHOT</quarkus.version>@<quarkus.version>${VERSION}</quarkus.version>@" extensions/azure-functions-http/maven-archetype/src/main/resources/archetype-resources/pom.xml
sed -i "s@<quarkus-plugin.version>999-SNAPSHOT</quarkus-plugin.version>@<quarkus-plugin.version>${VERSION}</quarkus-plugin.version>@" extensions/azure-functions-http/maven-archetype/src/main/resources/archetype-resources/pom.xml
sed -i "s@<quarkus.platform.version>999-SNAPSHOT</quarkus.platform.version>@<quarkus.platform.version>${VERSION}</quarkus.platform.version>@" extensions/azure-functions-http/maven-archetype/src/main/resources/archetype-resources/pom.xml

sed -i "s@<quarkus.version>999-SNAPSHOT</quarkus.version>@<quarkus.version>${VERSION}</quarkus.version>@" extensions/amazon-lambda/maven-archetype/src/main/resources/archetype-resources/pom.xml
sed -i "s@<quarkus-plugin.version>999-SNAPSHOT</quarkus-plugin.version>@<quarkus-plugin.version>${VERSION}</quarkus-plugin.version>@" extensions/amazon-lambda/maven-archetype/src/main/resources/archetype-resources/pom.xml
sed -i "s@<quarkus.platform.version>999-SNAPSHOT</quarkus.platform.version>@<quarkus.platform.version>${VERSION}</quarkus.platform.version>@" extensions/amazon-lambda/maven-archetype/src/main/resources/archetype-resources/pom.xml

sed -i "s@<quarkus.version>999-SNAPSHOT</quarkus.version>@<quarkus.version>${VERSION}</quarkus.version>@" extensions/amazon-lambda-http/maven-archetype/src/main/resources/archetype-resources/pom.xml
sed -i "s@<quarkus-plugin.version>999-SNAPSHOT</quarkus-plugin.version>@<quarkus-plugin.version>${VERSION}</quarkus-plugin.version>@" extensions/amazon-lambda-http/maven-archetype/src/main/resources/archetype-resources/pom.xml
sed -i "s@<quarkus.platform.version>999-SNAPSHOT</quarkus.platform.version>@<quarkus.platform.version>${VERSION}</quarkus.platform.version>@" extensions/amazon-lambda-http/maven-archetype/src/main/resources/archetype-resources/pom.xml

if [ -f extensions/funqy/funqy-amazon-lambda/maven-archetype/src/main/resources/archetype-resources/pom.xml ]; then
    sed -i "s@<quarkus.version>999-SNAPSHOT</quarkus.version>@<quarkus.version>${VERSION}</quarkus.version>@" extensions/funqy/funqy-amazon-lambda/maven-archetype/src/main/resources/archetype-resources/pom.xml
    sed -i "s@<quarkus-plugin.version>999-SNAPSHOT</quarkus-plugin.version>@<quarkus-plugin.version>${VERSION}</quarkus-plugin.version>@" extensions/funqy/funqy-amazon-lambda/maven-archetype/src/main/resources/archetype-resources/pom.xml
    sed -i "s@<quarkus.platform.version>999-SNAPSHOT</quarkus.platform.version>@<quarkus.platform.version>${VERSION}</quarkus.platform.version>@" extensions/funqy/funqy-amazon-lambda/maven-archetype/src/main/resources/archetype-resources/pom.xml
fi
if [ -f extensions/amazon-lambda-rest/maven-archetype/src/main/resources/archetype-resources/pom.xml ]; then
    sed -i "s@<quarkus.version>999-SNAPSHOT</quarkus.version>@<quarkus.version>${VERSION}</quarkus.version>@" extensions/amazon-lambda-rest/maven-archetype/src/main/resources/archetype-resources/pom.xml
    sed -i "s@<quarkus-plugin.version>999-SNAPSHOT</quarkus-plugin.version>@<quarkus-plugin.version>${VERSION}</quarkus-plugin.version>@" extensions/amazon-lambda-rest/maven-archetype/src/main/resources/archetype-resources/pom.xml
    sed -i "s@<quarkus.platform.version>999-SNAPSHOT</quarkus.platform.version>@<quarkus.platform.version>${VERSION}</quarkus.platform.version>@" extensions/amazon-lambda-rest/maven-archetype/src/main/resources/archetype-resources/pom.xml
fi
if [ -f coverage-report/pom.xml ]; then
    sed -i "s@<version>999-SNAPSHOT</version>@<version>${VERSION}</version>@" coverage-report/pom.xml
fi
