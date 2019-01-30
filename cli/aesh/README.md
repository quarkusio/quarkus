# Shamrock Cli 
Welcome to the WIP

# Generate native-image

Copy the dependency jars to target/dependency:
---
mvn dependency:copy-dependencies
---

Copy the cli jars to target/dependency:
---
cp target/shamrock-cli-1.0.0.Alpha1-SNAPSHOT.jar target/dependency/
cp target/original-shamrock-cli-1.0.0.Alpha1-SNAPSHOT.jar target/dependency/
---

Run:
---
$GRAAL_HOME/bin/native-image --verbose -H:+ReportUnsupportedElementsAtRuntime 
-H:ReflectionConfigurationFiles=./reflectconfigs/shamrockcli.json -jar target/dependency/shamrock-cli-1.0.0.Alpha1-SNAPSHOT.jar
---

which should generate a 'shamrock-cli-1.0.0.Alpha1-SNAPSHOT' binary.


