#!/usr/bin/env bash

# Requires JDK10; tested with:
# OpenJDK Runtime Environment (build 10.0.1+10)

## Start by running tests:
mvn clean install

## Update the classpath definition used by this script:
mvn dependency:build-classpath -Dmdep.outputFile=cp.txt > /dev/null
CLASSPATH=`cat cp.txt`

RUNTIME_OPTS="-Djava.util.logging.config.file=logging.properties"

echo "Using generated classpath: $CLASSPATH"

#with Java 9 jigsaw:
#java --module-path=./target/modules --upgrade-module-path=./target/upgrade-modules --add-modules=java.transaction,java.xml.bind --module=com.example/com.example.Main

#Test a traditional classpath run:
time java $RUNTIME_OPTS --add-modules java.xml.bind -cp "$CLASSPATH":./target/classes com.example.Main
echo "That was a traditional classpath run"
echo ""

#generating class definitions list
java $RUNTIME_OPTS -Xshare:off -XX:+UseAppCDS -XX:DumpLoadedClassList=classdef.lst --add-modules java.xml.bind -cp "$CLASSPATH":./target/classes com.example.Main

#generating binaries from the definition list (N.B. omitting the Main and entities code)
java $RUNTIME_OPTS -Xshare:dump -XX:+UseAppCDS -XX:SharedClassListFile=classdef.lst -XX:SharedArchiveFile=classdef.jsa --add-modules java.xml.bind -cp "$CLASSPATH"

#try it once:
java $RUNTIME_OPTS -Xshare:on -XX:+UseAppCDS -XX:SharedArchiveFile=classdef.jsa --add-modules java.xml.bind -cp "$CLASSPATH":./target/classes com.example.Main
echo "That was a run with shared class definitions"
echo ""


##Verify

#java -Xlog:class+load=info -Xshare:on -XX:+UseAppCDS -XX:SharedArchiveFile=classdef.jsa --add-modules java.xml.bind -cp "$CLASSPATH":./target/classes com.example.Main | grep -v "shared objects file"




