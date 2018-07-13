#!/usr/bin/env bash

echo "Expects GraalVM on classpath"
echo ""

echo "Starting mvn build..."
mvn clean package > maven-build.log

## Update the classpath definition used by this script:
mvn dependency:build-classpath -DexcludeArtifactIds=svm-core -Dmdep.outputFile=cp.txt >> maven-build.log
CLASSPATH=`cat cp.txt`

echo "Starting native-image :"
#--verbose --shared -ea -H:+ReportUnsupportedElementsAtRuntime -H:+PrintAnalysisCallTree
native-image --no-server -O0 --verbose -H:IncludeResources=META-INF/persistence.xml -H:+ReportUnsupportedElementsAtRuntime -H:ReflectionConfigurationFiles=reflectconfig.json -cp "$CLASSPATH":./target/classes com.example.Main com.example.Main

echo ""
echo ""
echo "Starting the native app:"

./com.example.Main