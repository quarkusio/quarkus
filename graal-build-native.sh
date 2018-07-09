#!/usr/bin/env bash

echo "Expects GraalVM on classpath"
echo ""

echo "Starting mvn build..."
mvn clean install > maven-build.log

## Update the classpath definition used by this script:
mvn dependency:build-classpath -Dmdep.outputFile=cp.txt >> maven-build.log
CLASSPATH=`cat cp.txt`

echo "Starting native-image :"

native-image -cp "$CLASSPATH":./target/classes com.example.Main com.example.Main

echo ""
echo "Starting the native app:"

./com.example.Main