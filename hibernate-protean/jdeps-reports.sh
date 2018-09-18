#!/usr/bin/env bash

# Expects JDK 10+ on classpath

## Update the classpath definition used by this script:
mvn dependency:build-classpath -Dmdep.outputFile=cp.txt > /dev/null
CLASSPATH=`cat cp.txt`

echo "Listing `jdeps --print-module-deps` for each dependency individually:"
IFS=':'; CLASSPATH_ARRAY=($CLASSPATH); unset IFS;
for dep in ${CLASSPATH_ARRAY[@]}
do
	echo "  "
    echo "Dependency: $dep"
    jdeps --print-module-deps $dep
done
echo "  "
# TODO : combined report not supported by jdeps?
#echo "Listing `jdeps --print-module-deps` for all dependencies combined:"
#jdeps --print-module-deps -cp $CLASSPATH



