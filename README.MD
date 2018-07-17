# Hibernate ORM for Protean

Experiments to run Hibernate ORM on:
 - SubstrateVM
 - traditional JVM with Graal
 - experiments with Class Data Sharing
 - experiments with Jigsaw modules
 
 The main focus is on `SubstrateVM`, each of these have a different bash script.
 
 ## Dependencies
 - A build of https://github.com/protean-project/hibernate-orm
 
      > ./gradlew clean publishToMavenLocal
     
(From the hibernate-orm checkout directory)

 - Requires any JDK8+ to build it.

## Test on SubstrateVM

      > ./graal-build-native.sh

(from the checkout of this project)

 - Requires Graal on PATH and on JAVA_HOME, et al.
 - requires a revent Maven version

## Status

Work in progress.. gets quite far in the boot process but eventually fails.