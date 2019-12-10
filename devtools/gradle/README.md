Quarkus Gradle Plugin
=====================

Builds a Quarkus application, and provides helpers to launch dev-mode, the Quarkus CLI and the build of native images.

Releases are published at https://plugins.gradle.org/plugin/io.quarkus .

Functional Tests
----------------

To run the functional tests, run the following command:

```bash
./gradlew functionalTests
```

Local development
-----------------

1. Build the entire Quarkus codebase by running `mvn clean install -DskipTests -DskipITs` in the project root 
    - This should install the Gradle plugin in your local maven repository.

2. Create a sample project using the Maven plugin:

```bash
    mvn io.quarkus:quarkus-maven-plugin:999-SNAPSHOT:create \
        -DprojectGroupId=org.acme \
        -DprojectArtifactId=my-gradle-project \
        -DclassName="org.acme.quickstart.GreetingResource" \
        -DplatformArtifactId=quarkus-bom \
        -Dpath="/hello" \
        -DbuildTool=gradle
```

Follow the instructions in the [Gradle Tooling Guide](https://quarkus.io/guides/gradle-tooling) for more information about the available commands.

Importing using Intellij
-------------------------

Disable "Maven Auto Import" for the Quarkus projects. Since the Gradle plugin has a pom.xml,
IntelliJ will configure this project as a Maven project. You need to configure it to be a Gradle
project. To do so, follow these instructions:


1. Go to File -> Project Structure
2. In Modules, remove the `quarkus-gradle-plugin` and re-import as a Gradle project.
