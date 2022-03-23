#!/bin/bash

# This script is used to gradually transform Quarkus bits from javax to jakarta namespaces and update dependencies

# Each transformed module/project is expected to:
# a) execute Eclipse Transformer command to transform relevant directory
# b) update dependencies to their respective EE 9 versions
# c) add a build and test command that will verify the functionality

quarkusPath="$(pwd)"
echo "Path to quarkus repo is: $quarkusPath"

BOM="$quarkusPath/bom/application/pom.xml"

# Set up jbang alias, we are using latest released transformer version
jbang alias add --name transform org.eclipse.transformer:org.eclipse.transformer.cli:0.2.0

# Function to help transform a particular Maven module using Eclipse Transformer
transform_module () {
  local modulePath="$quarkusPath/$1"
  local transformationTemp="$quarkusPath/JAKARTA_TEMP"
  rm -Rf $transformationTemp
  mkdir $transformationTemp
  echo "Transforming $modulePath"
  jbang transform -o $modulePath $transformationTemp
  rm -Rf "$modulePath"
  mv "$transformationTemp" "$modulePath"
  echo "Transformation done"
}

# Build, test and install a particular maven module (chosen by relative path)
build_module () {
  local pomPath="$quarkusPath/$1/pom.xml"
  ./mvnw -B clean install -f "$pomPath"
  echo "Installed newly built $pomPath"
}

# Arc project transformation
transform_module "independent-projects/arc"

# Now we need to update CDI, JTA, JPA and common annotations artifacts
sed -i 's/<version.cdi>2.0.2<\/version.cdi>/<version.cdi>3.0.0<\/version.cdi>/g' "$quarkusPath/independent-projects/arc/pom.xml"
sed -i 's/<version.jta>1.3.3<\/version.jta>/<version.jta>2.0.0<\/version.jta>/g' "$quarkusPath/independent-projects/arc/pom.xml"
sed -i 's/<version.jakarta-annotation>1.3.5<\/version.jakarta-annotation>/<version.jakarta-annotation>2.0.0<\/version.jakarta-annotation>/g' "$quarkusPath/independent-projects/arc/pom.xml"
sed -i 's/<version.jpa>2.2.3<\/version.jpa>/<version.jpa>3.0.0<\/version.jpa>/g' "$quarkusPath/independent-projects/arc/pom.xml"

# Test & install modified Arc
build_module "independent-projects/arc"

# Switch parent BOM to Jakarta artifacts

sed -i 's/<jakarta.inject-api.version>1.0<\/jakarta.inject-api.version>/<jakarta.inject-api.version>2.0.0<\/jakarta.inject-api.version>/g' $BOM
sed -i 's/<jakarta.interceptor-api.version>1.2.5<\/jakarta.interceptor-api.version>/<jakarta.interceptor-api.version>2.0.0<\/jakarta.interceptor-api.version>/g' $BOM
sed -i 's/<jakarta.transaction-api.version>1.3.3<\/jakarta.transaction-api.version>/<jakarta.transaction-api.version>2.0.0<\/jakarta.transaction-api.version>/g' $BOM
sed -i 's/<jakarta.enterprise.cdi-api.version>2.0.2<\/jakarta.enterprise.cdi-api.version>/<jakarta.enterprise.cdi-api.version>3.0.0<\/jakarta.enterprise.cdi-api.version>/g' $BOM
sed -i 's/<jakarta.annotation-api.version>1.3.5<\/jakarta.annotation-api.version>/<jakarta.annotation-api.version>2.0.0<\/jakarta.annotation-api.version>/g' $BOM
sed -i 's/<jakarta.persistence-api.version>2.2.3<\/jakarta.persistence-api.version>/<jakarta.persistence-api.version>3.0.0<\/jakarta.persistence-api.version>/g' $BOM

# Install the modified BOM:
build_module "bom/application"

## Arc Extension [Incomplete: other modules need to go first]

# transform_module "extensions/arc/runtime"
# transform_module "extensions/arc/deployment"
# build_module "extensions/arc/runtime"
# build_module "extensions/arc/deployment"
