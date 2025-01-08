
mvncmd := env_var_or_default("QMVNCMD", "./mvnw -T0.8C")

# build main project quickly
build:
   {{mvncmd}} -Dquickly

# build main project fast - skip docs, tests, ITs, invoker, extension validation, gradle tests, truststore
build-fast:
    {{mvncmd}} -e -DskipDocs -DskipTests -DskipITs -Dinvoker.skip -DskipExtensionValidation -Dskip.gradle.tests -Dtruststore.skip clean install

# build docs (including config doc for all modules), skipping as much unnecessary as possible. 
build-docs:
    {{mvncmd}} -e -DskipTests -DskipITs -Dinvoker.skip -DskipExtensionValidation -Dskip.gradle.tests -Dtruststore.skip -Dno-test-modules -Dasciidoctor.fail-if=DEBUG clean install

# format code according to Quarkus coding conventions
format:
    {{mvncmd}} process-sources -Denforcer.skip -Dprotoc.skip

# run Quarkus CLI from locally built snapshot
qss:
    java -jar ./devtools/cli/target/quarkus-cli-999-SNAPSHOT-runner.jar
