#!/bin/bash

# This script is used to gradually transform Quarkus bits from javax to jakarta namespaces and update dependencies

# Each transformed module/project is expected to:
# a) execute Eclipse Transformer command to transform relevant directory
# b) update dependencies to their respective EE 9 versions
# c) add a build and test command that will verify the functionality

quarkusPath="$(pwd)"
echo "Path to quarkus repo is: $quarkusPath"

# Set up jbang alias, we are using latest released transformer version
jbang alias add --name transform org.eclipse.transformer:org.eclipse.transformer.cli:0.2.0

# Arc project transformation
jbang transform "$quarkusPath/independent-projects/arc"

# Replace old sources with newly generated ones
rm -rf "$quarkusPath/independent-projects/arc"
mv "$quarkusPath/independent-projects/output_arc" "$quarkusPath/independent-projects/arc"

# Now we need to update CDI, JTA, JPA and common annotations artifacts
sed -i 's/<version.cdi>2.0.2<\/version.cdi>/<version.cdi>3.0.0<\/version.cdi>/g' "$quarkusPath/independent-projects/arc/pom.xml"
sed -i 's/<version.jta>1.3.3<\/version.jta>/<version.jta>2.0.0<\/version.jta>/g' "$quarkusPath/independent-projects/arc/pom.xml"
sed -i 's/<version.jakarta-annotation>1.3.5<\/version.jakarta-annotation>/<version.jakarta-annotation>2.0.0<\/version.jakarta-annotation>/g' "$quarkusPath/independent-projects/arc/pom.xml"
sed -i 's/<version.jpa>2.2.3<\/version.jpa>/<version.jpa>3.0.0<\/version.jpa>/g' "$quarkusPath/independent-projects/arc/pom.xml"

# Execute build and tests to verify functionality
./mvnw -B clean install -f "$quarkusPath/independent-projects/arc/pom.xml"


