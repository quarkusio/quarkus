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

# Sets the EDITING variable to the file being edited by set_property
edit_begin () {
  EDITING="$quarkusPath/$1"
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

# Arc project transformation
transform_module "independent-projects/arc"

# Now we need to update CDI, JTA, JPA and common annotations artifacts
edit_begin "independent-projects/arc/pom.xml"
set_property "version.cdi" "3.0.0"
set_property "version.jta" "2.0.0"
set_property "version.jakarta-annotation" "2.0.0"
set_property "version.jpa" "3.0.0"

# Test & install modified Arc
build_module "independent-projects/arc"

# Switch parent BOM to Jakarta artifacts
edit_begin "bom/application/pom.xml"
set_property "jakarta.inject-api.version" "2.0.0"
set_property "jakarta.interceptor-api.version" "2.0.0"
set_property "jakarta.transaction-api.version" "2.0.0"
set_property "jakarta.enterprise.cdi-api.version" "3.0.0"
set_property "jakarta.annotation-api.version" "2.0.0"
set_property "jakarta.persistence-api.version" "3.0.0"

# Install the modified BOM:
build_module "bom/application"

## Arc Extension [Incomplete: other modules need to go first]

# transform_module "extensions/arc/runtime"
# transform_module "extensions/arc/deployment"
# build_module "extensions/arc/runtime"
# build_module "extensions/arc/deployment"
