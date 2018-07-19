# Hibernate ORM for Protean

Experiments to run Hibernate ORM on:
 - SubstrateVM
 - traditional JVM with Graal
 - experiments with Class Data Sharing
 - experiments with Jigsaw modules
 
The main focus is on `SubstrateVM`, each of these have a different bash script.

For Protean you can ignore the other scripts; focus on `graal-build-native.sh`.
 
## Dependencies

Some custom builds are needed as dependencies.

### Patched Hibernate ORM
 
A build of https://github.com/protean-project/hibernate-orm
 
      > git clone git@github.com:protean-project/hibernate-orm.git
      > cd hibernate-orm
      > git checkout ProteanHacking
      > ./gradlew clean publishToMavenLocal

 - N.B. the `ProteanHacking` branch.
 - Requires any JDK8+ to build it.
 
 ### Very latest PostgreSQL JDBC driver
 
     > git clone git://github.com/pgjdbc/pgjdbc.git
     > cd pgjdbc
     > mvn install -DskipTests

 - Tested at commit `178eecc90643b36c8c5cd423ff311b26733384f2`
 - Requires any JDK8+ to build it.

### Graal annotations

We need the Graal annotations `com.oracle.svm.core.annotate.Substitute`, `com.oracle.svm.core.annotate.TargetClass`, etc..

These are found in the `svm-core.jar` from the substrate builds. Find the jar and install it in your local Maven repo with the following coordinates:

    mvn install:install-file -Dfile=/wherever/graal/substratevm/mxbuild/dists/svm-core.jar -DgroupId=graal-local-builds -DartifactId=svm-core -Dversion=1.0.0-SNAPSHOT -Dpackaging=jar


# Test on SubstrateVM

Now you can invoke the script from the checkout folder of this project:

      > ./graal-build-native.sh

 - Requires Graal on PATH and on JAVA_HOME, et al.
 - requires a recent Maven version

## Status

It should be able to successfully complete the `native-image` creation process.

Actually booting the resulting binary will fail; work in progress!