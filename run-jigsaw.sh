#!/usr/bin/env bash

# Requires JDK 10 !

JIGSAW_CLASS_LIST=jigsaw-class.lst
JIGSAW_CLASS_BINARY=jigsaw-classdump.jsa

#with Java 9 jigsaw:
#java --module-path=./target/modules --upgrade-module-path=./target/upgrade-modules --add-modules=java.transaction,java.xml.bind --module=com.example/com.example.Main

#Traditional classpath, generated via mvn dependency:build-classpath , then adding java.xml.bind module and current /target/classes :
#java --add-modules java.xml.bind -cp /home/sanne/.m2/repository/org/hibernate/hibernate-core/5.3.2-SNAPSHOT/hibernate-core-5.3.2-SNAPSHOT.jar:/home/sanne/.m2/repository/org/jboss/logging/jboss-logging/3.3.2.Final/jboss-logging-3.3.2.Final.jar:/home/sanne/.m2/repository/javax/persistence/javax.persistence-api/2.2/javax.persistence-api-2.2.jar:/home/sanne/.m2/repository/org/javassist/javassist/3.22.0-GA/javassist-3.22.0-GA.jar:/home/sanne/.m2/repository/net/bytebuddy/byte-buddy/1.8.12/byte-buddy-1.8.12.jar:/home/sanne/.m2/repository/antlr/antlr/2.7.7/antlr-2.7.7.jar:/home/sanne/.m2/repository/org/jboss/spec/javax/transaction/jboss-transaction-api_1.2_spec/2.0.0.Alpha1/jboss-transaction-api_1.2_spec-2.0.0.Alpha1.jar:/home/sanne/.m2/repository/org/jboss/jandex/2.0.3.Final/jandex-2.0.3.Final.jar:/home/sanne/.m2/repository/com/fasterxml/classmate/1.3.4/classmate-1.3.4.jar:/home/sanne/.m2/repository/dom4j/dom4j/1.6.1/dom4j-1.6.1.jar:/home/sanne/.m2/repository/org/hibernate/common/hibernate-commons-annotations/5.0.3.Final/hibernate-commons-annotations-5.0.3.Final.jar:/home/sanne/.m2/repository/com/h2database/h2/1.4.196/h2-1.4.196.jar:/home/sanne/.m2/repository/log4j/log4j/1.2.17/log4j-1.2.17.jar:./target/classes com.example.Main



#generating class definitions list
java -Xshare:off -XX:+UseAppCDS -XX:DumpLoadedClassList="$JIGSAW_CLASS_LIST" --module-path=./target/modules --upgrade-module-path=./target/upgrade-modules --add-modules=java.transaction,java.xml.bind --module=com.example/com.example.Main


#generating binaries from the definition list (N.B. omitting the Main and entities code)
java -Xshare:dump -XX:+UseAppCDS -XX:SharedClassListFile="$JIGSAW_CLASS_LIST" -XX:SharedArchiveFile="$JIGSAW_CLASS_BINARY" --module-path=./target/modules --upgrade-module-path=./target/upgrade-modules --add-modules=java.transaction,java.xml.bind

#Profit?
#java -Xshare:on -XX:+UseAppCDS -XX:SharedArchiveFile=classdef.jsa --module-path=./target/modules --upgrade-module-path=./target/upgrade-modules --add-modules=java.transaction,java.xml.bind --module=com.example/com.example.Main

#Verify where stuff is loaded from:
#java -Xlog:class+load=info -Xshare:on -XX:+UseAppCDS -XX:SharedArchiveFile=classdef.jsa --module-path=./target/modules --upgrade-module-path=./target/upgrade-modules --add-modules=java.transaction,java.xml.bind --module=com.example/com.example.Main


