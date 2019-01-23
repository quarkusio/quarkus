# Shamrock Cli 
welcome to the WIP

# Generate native-image

Copy the dependency jars to target/
cp ~/.m2/repository/org/codehaus/plexus/plexus-utils/3.1.0/plexus-utils-3.1.0.jar target/
cp ~/.m2/repository/org/apache/maven/maven-model/3.6.0/maven-model-3.6.0.jar target/
cp ~/.m2/repository/org/fusesource/jansi/jansi/1.17/jansi-1.17.jar target/
cp ~/.m2/repository/org/aesh/aesh-readline/1.13/aesh-readline-1.13.jar target/
cp ~/.m2/repository/org/aesh/aesh/1.10/aesh-1.10.jar target/

Run:
$GRAAL_HOME/bin/native-image --verbose -H:+ReportUnsupportedElementsAtRuntime -H:ReflectionConfigurationFiles=./reflectconfigs/shamrockcli.json -jar target/shamrock-cli-1.0.0.Alpha1-SNAPSHOT.jar

which should generate a 'shamrock-cli-1.0.0.Alpha1-SNAPSHOT' binary.


