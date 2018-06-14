#!/usr/bin/env bash

# Requires JDK 10 !

## Start by running tests:
#mvn clean install

## Update the classpath definition used by this script:
#mvn dependency:build-classpath -Dmdep.outputFile=cp.txt > /dev/null
CLASSPATH=`cat cp.txt`

RUNTIME_OPTS="-Djava.util.logging.config.file=logging.properties"

echo "Using generated classpath: $CLASSPATH"

# Usage note:
# jdeps --print-module-deps /home/sanne/.m2/repository/org/hibernate/common/hibernate-commons-annotations/5.0.4.Final/hibernate-commons-annotations-5.0.4.Final.jar
# > java.base

# jdeps --print-module-deps /home/sanne/.m2/repository/org/postgresql/postgresql/42.2.2/postgresql-42.2.2.jar
# > java.base,java.desktop,java.naming,java.security.jgss,java.security.sasl,java.sql
# [Fail: ClassNotFoundException: com.sun.jna.win32.StdCallLibrary]

# classmate -> java.base
# [ OK!! AOT compatible?! ]

# jboss-logging -> java.base, java.logging
# [Fail: ClassNotFoundException: org.jboss.logmanager.Level]

# dom4j -> NPE! [fail: NoClassDefFoundError: org/jaxen/VariableContext]

# javax.persistence-api-2.2.jar -> java.base,java.instrument,java.sql
# [ OK!! AOT compatible?! ]

# jboss-transaction-api_1.2_spec -> java.base,java.rmi
# [ OK!! AOT compatible?! ]

# byte-buddy -> java.base,java.instrument
# [Fail: NoClassDefFoundError: net/bytebuddy/jar/asm/commons/SignatureRemapper]

# hibernate-orm -> java.base,java.desktop,java.instrument,java.management,java.naming,java.sql,java.xml.bind

# jandex => java.base
# [Fail: ClassNotFoundException: org.apache.tools.ant.Task]

# [java.xml.bind -> fails as well!]

jaotc -J-XX:+UseCompressedOops -J-XX:+UseG1GC -J-Xmx16g --compile-for-tiered --info --compile-commands java.base-list.txt --output libjava.hib-custom.so --module java.base --module java.logging --module java.naming --module java.sql --module java.security.jgss --module java.security.sasl --module java.instrument --module java.management /home/sanne/.m2/repository/org/jboss/spec/javax/transaction/jboss-transaction-api_1.2_spec/2.0.0.Alpha1/jboss-transaction-api_1.2_spec-2.0.0.Alpha1.jar /home/sanne/.m2/repository/javax/persistence/javax.persistence-api/2.2/javax.persistence-api-2.2.jar /home/sanne/.m2/repository/com/fasterxml/classmate/1.3.4/classmate-1.3.4.jar

strip libjava.hib-custom.so
# oops: 317M	libjava.hib-custom.so

time java $RUNTIME_OPTS -Xshare:on -XX:+UseAppCDS -XX:AOTLibrary=./libjava.hib-custom.so -XX:SharedArchiveFile=classdef.jsa --add-modules java.xml.bind -cp "$CLASSPATH":./target/classes com.example.Main