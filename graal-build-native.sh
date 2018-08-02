#!/usr/bin/env bash

TIME_FORMAT="Elapsed real time (s): %e \tMaximum resident set size of the process (KB): %M"

shopt -s expand_aliases
if [[ "$OSTYPE" == "darwin"* ]]; then
    alias timer="/usr/bin/time -lp "
else
    alias timer="/usr/bin/time -a -f \"$TIME_FORMAT\""
fi

echo "Expects GraalVM on classpath"
echo ""

echo "Starting mvn build..."
mvn clean package > maven-build.log

## Update the classpath definition used by this script:
mvn dependency:build-classpath -DexcludeArtifactIds=graal-annotations -Dmdep.outputFile=cp.txt >> maven-build.log
CLASSPATH=`cat cp.txt`

echo "Removing previous binary, to avoid misleading in case native-image fails:"
rm com.example.Main

echo "Starting native-image :"
#--verbose --shared -ea -H:+ReportUnsupportedElementsAtRuntime -H:+PrintAnalysisCallTree -R:±LSRAOptimization -R:AnalysisSizeCutoff=8 -H:-RuntimeAssertions -H:+PrintImageElementSizes -H:+PrintImageHeapPartitionSizes -H:+PrintImageObjectTree -H:+PrintAnalysisCallTree -H:+ReportUnsupportedElementsAtRuntime
timer native-image --no-server -O1 --verbose -H:IncludeResources=META-INF/services/.* -H:Kind=EXECUTABLE -H:ReflectionConfigurationFiles=reflectconfig.json -cp "$CLASSPATH":./target/classes com.example.Main com.example.Main

echo ""
echo "Disk size, before strip:"
du -h com.example.Main
echo "Disk size, after strip:"
strip com.example.Main
du -h com.example.Main

echo ""
echo ""
echo "Starting the native app:"

timer ./com.example.Main -Xmx1M -Xmn1M


# TODO check benefits of fully static binaries? Could seriously trim the base image?
#ldd com.example.Main
