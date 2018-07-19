#!/usr/bin/env bash

TIME_FORMAT="Seconds: %e \tMax memory (KB): %M"

shopt -s expand_aliases
alias timer="/usr/bin/time -a -f \"$TIME_FORMAT\""

echo "Expects GraalVM on classpath"
echo ""

echo "Starting mvn build..."
mvn clean package > maven-build.log

## Update the classpath definition used by this script:
mvn dependency:build-classpath -DexcludeArtifactIds=svm-core -Dmdep.outputFile=cp.txt >> maven-build.log
CLASSPATH=`cat cp.txt`

echo "Removing previous binary, to avoid misleading in case native-image fails:"
rm com.example.Main

echo "Starting native-image :"
#--verbose --shared -ea -H:+ReportUnsupportedElementsAtRuntime -H:+PrintAnalysisCallTree
timer native-image --no-server -O0 --verbose -H:IncludeResources=META-INF/services/.* -H:-RuntimeAssertions -H:Kind=EXECUTABLE -H:+ReportUnsupportedElementsAtRuntime -H:ReflectionConfigurationFiles=reflectconfig.json -cp "$CLASSPATH":./target/classes com.example.Main com.example.Main

echo ""
echo ""
echo "Starting the native app:"

timer ./com.example.Main

echo ""
echo "Disk size, before strip:"
du -h com.example.Main
echo "Disk size, after strip:"
strip com.example.Main
du -h com.example.Main

# TODO check benefits of fully static binaries? Could seriously trim the base image?
#ldd com.example.Main
