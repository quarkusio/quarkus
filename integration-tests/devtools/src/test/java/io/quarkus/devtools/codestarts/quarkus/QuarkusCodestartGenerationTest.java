package io.quarkus.devtools.codestarts.quarkus;

import static io.quarkus.devtools.codestarts.quarkus.QuarkusCodestartData.QuarkusDataKey.*;
import static io.quarkus.devtools.testing.SnapshotTesting.assertThatMatchSnapshot;
import static io.quarkus.devtools.testing.SnapshotTesting.checkContains;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import io.quarkus.bootstrap.model.AppArtifactCoords;
import io.quarkus.bootstrap.model.AppArtifactKey;
import io.quarkus.devtools.PlatformAwareTestBase;
import io.quarkus.devtools.project.BuildTool;
import io.quarkus.devtools.testing.SnapshotTesting;

class QuarkusCodestartGenerationTest extends PlatformAwareTestBase {

    private static final Path testDirPath = Paths.get("target/quarkus-codestart-gen-test");

    @BeforeAll
    static void setUp() throws Throwable {
        SnapshotTesting.deleteTestDirectory(testDirPath.toFile());
    }

    private Map<String, Object> getGenerationTestInputData() {
        return getGenerationTestInputData(null);
    }

    private static Map<String, Object> getGenerationTestInputData(final Map<String, Object> override) {
        final HashMap<String, Object> data = new HashMap<>();
        data.put(PROJECT_GROUP_ID.key(), "org.test");
        data.put(PROJECT_ARTIFACT_ID.key(), "test-codestart");
        data.put(PROJECT_VERSION.key(), "1.0.0-codestart");
        data.put(BOM_GROUP_ID.key(), "io.quarkus");
        data.put(BOM_ARTIFACT_ID.key(), "quarkus-mock-bom");
        data.put(BOM_VERSION.key(), "999-MOCK");
        data.put(QUARKUS_VERSION.key(), "999-MOCK");
        data.put(QUARKUS_MAVEN_PLUGIN_GROUP_ID.key(), "io.quarkus");
        data.put(QUARKUS_MAVEN_PLUGIN_ARTIFACT_ID.key(), "quarkus-mock-maven-plugin");
        data.put(QUARKUS_MAVEN_PLUGIN_VERSION.key(), "999-MOCK");
        data.put(QUARKUS_GRADLE_PLUGIN_ID.key(), "io.quarkus");
        data.put(QUARKUS_GRADLE_PLUGIN_VERSION.key(), "999-MOCK");
        data.put(JAVA_VERSION.key(), "11");
        data.put(KOTLIN_VERSION.key(), "1.4.28-MOCK");
        data.put(SCALA_VERSION.key(), "2.12.8-MOCK");
        data.put(SCALA_MAVEN_PLUGIN_VERSION.key(), "4.1.1-MOCK");
        data.put(MAVEN_COMPILER_PLUGIN_VERSION.key(), "3.8.1-MOCK");
        data.put(MAVEN_SUREFIRE_PLUGIN_VERSION.key(), "3.0.0-MOCK");
        if (override != null)
            data.putAll(override);
        return data;
    }

    @Test
    void generateDefault(TestInfo testInfo) throws Throwable {
        final QuarkusCodestartProjectInput input = QuarkusCodestartProjectInput.builder()
                .noExamples()
                .noDockerfiles()
                .noBuildToolWrapper()
                .addData(getGenerationTestInputData())
                .build();
        final Path projectDir = testDirPath.resolve("default");
        getCatalog().createProject(input).generate(projectDir);

        checkMaven(projectDir);
        checkReadme(projectDir);

        assertThat(projectDir.resolve(".mvnw")).doesNotExist();
        assertThat(projectDir.resolve(".dockerignore")).doesNotExist();

        assertThatMatchSnapshot(testInfo, projectDir, "pom.xml");
        assertThatMatchSnapshot(testInfo, projectDir, "README.md");

        assertThat(projectDir.resolve("src/main/java")).exists().isEmptyDirectory();
    }

    @Test
    void generateCommandMode(TestInfo testInfo) throws Throwable {
        final QuarkusCodestartProjectInput input = QuarkusCodestartProjectInput.builder()
                .addCodestart("commandmode")
                .addData(getGenerationTestInputData())
                .build();

        final Path projectDir = testDirPath.resolve("commandmode");
        getCatalog().createProject(input).generate(projectDir);

        checkMaven(projectDir);
        checkReadme(projectDir);
        checkDockerfiles(projectDir, BuildTool.MAVEN);
        checkConfigProperties(projectDir);

        assertThatMatchSnapshot(testInfo, projectDir, "src/main/java/org/acme/HelloCommando.java");
    }

    @Test
    void generateCommandModeCustom(TestInfo testInfo) throws Throwable {
        final QuarkusCodestartProjectInput input = QuarkusCodestartProjectInput.builder()
                .addCodestart("commandmode")
                .addData(getGenerationTestInputData())
                .putData(PROJECT_PACKAGE_NAME.key(), "com.test.andy")
                .putData(COMMANDMODE_EXAMPLE_RESOURCE_CLASS_NAME.key(), "AndyCommando")
                .build();
        final Path projectDir = testDirPath.resolve("commandmode-custom");
        getCatalog().createProject(input).generate(projectDir);

        checkMaven(projectDir);
        checkReadme(projectDir);
        checkDockerfiles(projectDir, BuildTool.MAVEN);
        checkConfigProperties(projectDir);

        assertThatMatchSnapshot(testInfo, projectDir, "src/main/java/com/test/andy/AndyCommando.java");
    }

    @Test
    void generateRESTEasyJavaCustom(TestInfo testInfo) throws Throwable {
        final QuarkusCodestartProjectInput input = QuarkusCodestartProjectInput.builder()
                .addData(getGenerationTestInputData())
                .addExtension(AppArtifactKey.fromString("io.quarkus:quarkus-resteasy"))
                .putData(PROJECT_PACKAGE_NAME.key(), "com.andy")
                .putData(RESTEASY_EXAMPLE_RESOURCE_CLASS_NAME.key(), "BonjourResource")
                .putData(RESTEASY_EXAMPLE_RESOURCE_PATH.key(), "/bonjour")
                .build();
        final Path projectDir = testDirPath.resolve("resteasy-java-custom");
        getCatalog().createProject(input).generate(projectDir);

        checkMaven(projectDir);
        checkReadme(projectDir);
        checkDockerfiles(projectDir, BuildTool.MAVEN);
        checkConfigProperties(projectDir);

        assertThatMatchSnapshot(testInfo, projectDir, "src/main/java/com/andy/BonjourResource.java");
        assertThatMatchSnapshot(testInfo, projectDir, "src/test/java/com/andy/NativeBonjourResourceIT.java");
        assertThatMatchSnapshot(testInfo, projectDir, "src/main/resources/META-INF/resources/index.html");
    }

    @Test
    void generateRESTEasySpringWeb(TestInfo testInfo) throws Throwable {
        final QuarkusCodestartProjectInput input = QuarkusCodestartProjectInput.builder()
                .addData(getGenerationTestInputData())
                .addExtension(AppArtifactKey.fromString("io.quarkus:quarkus-resteasy"))
                .addExtension(AppArtifactKey.fromString("io.quarkus:quarkus-spring-web"))
                .build();
        final Path projectDir = testDirPath.resolve("resteasy-springweb");
        getCatalog().createProject(input).generate(projectDir);

        checkMaven(projectDir);
        checkReadme(projectDir);
        checkDockerfiles(projectDir, BuildTool.MAVEN);
        checkConfigProperties(projectDir);

        assertThatMatchSnapshot(testInfo, projectDir, "src/main/java/org/acme/GreetingResource.java");
        assertThatMatchSnapshot(testInfo, projectDir, "src/test/java/org/acme/GreetingResourceTest.java");
        assertThatMatchSnapshot(testInfo, projectDir, "src/main/java/org/acme/GreetingResource.java");
        assertThatMatchSnapshot(testInfo, projectDir, "src/main/java/org/acme/GreetingResource.java");
        assertThatMatchSnapshot(testInfo, projectDir, "src/test/java/org/acme/GreetingResourceTest.java");
        assertThatMatchSnapshot(testInfo, projectDir, "src/test/java/org/acme/NativeGreetingResourceIT.java");

        assertThatMatchSnapshot(testInfo, projectDir, "src/main/java/org/acme/SpringGreetingController.java");
        assertThatMatchSnapshot(testInfo, projectDir, "src/test/java/org/acme/SpringGreetingControllerTest.java");
        assertThatMatchSnapshot(testInfo, projectDir, "src/test/java/org/acme/NativeSpringGreetingControllerIT.java");

        assertThatMatchSnapshot(testInfo, projectDir, "src/main/resources/META-INF/resources/index.html")
                .satisfies(checkContains("\"/hello-resteasy\""))
                .satisfies(checkContains("quarkus.io/guides/rest-json"))
                .satisfies(checkContains("\"/hello-spring\""))
                .satisfies(checkContains("quarkus.io/guides/spring-web"));
    }

    @Test
    void generateMavenWithCustomDep(TestInfo testInfo) throws Throwable {
        final QuarkusCodestartProjectInput input = QuarkusCodestartProjectInput.builder()
                .addData(getGenerationTestInputData())
                .addExtension(AppArtifactKey.fromString("io.quarkus:quarkus-resteasy"))
                .addExtension(AppArtifactCoords.fromString("commons-io:commons-io:2.5"))

                .build();
        final Path projectDir = testDirPath.resolve("maven-custom-dep");
        getCatalog().createProject(input).generate(projectDir);

        checkMaven(projectDir);
        assertThatMatchSnapshot(testInfo, projectDir, "pom.xml")
                .satisfies(checkContains("<dependency>\n" +
                        "      <groupId>commons-io</groupId>\n" +
                        "      <artifactId>commons-io</artifactId>\n" +
                        "      <version>2.5</version>\n" +
                        "    </dependency>\n"))
                .satisfies(checkContains("<dependency>\n" +
                        "      <groupId>io.quarkus</groupId>\n" +
                        "      <artifactId>quarkus-resteasy</artifactId>\n" +
                        "    </dependency>\n"));
    }

    @Test
    void generateRESTEasyKotlinCustom(TestInfo testInfo) throws Throwable {
        final QuarkusCodestartProjectInput input = QuarkusCodestartProjectInput.builder()
                .addData(getGenerationTestInputData())
                .addExtension(AppArtifactKey.fromString("io.quarkus:quarkus-resteasy"))
                .addExtension(AppArtifactKey.fromString("io.quarkus:quarkus-kotlin"))
                .putData(PROJECT_PACKAGE_NAME.key(), "com.andy")
                .putData(RESTEASY_EXAMPLE_RESOURCE_CLASS_NAME.key(), "BonjourResource")
                .putData(RESTEASY_EXAMPLE_RESOURCE_PATH.key(), "/bonjour")
                .build();
        final Path projectDir = testDirPath.resolve("resteasy-kotlin-custom");
        getCatalog().createProject(input).generate(projectDir);

        checkMaven(projectDir);
        checkReadme(projectDir);
        checkDockerfiles(projectDir, BuildTool.MAVEN);
        checkConfigProperties(projectDir);

        assertThatMatchSnapshot(testInfo, projectDir, "src/main/kotlin/com/andy/BonjourResource.kt")
                .satisfies(checkContains("package com.andy"))
                .satisfies(checkContains("class BonjourResource"))
                .satisfies(checkContains("@Path(\"/bonjour\")"));

        assertThatMatchSnapshot(testInfo, projectDir, "src/test/kotlin/com/andy/BonjourResourceTest.kt")
                .satisfies(checkContains("package com.andy"))
                .satisfies(checkContains("class BonjourResourceTest"))
                .satisfies(checkContains("\"/bonjour\""));

        assertThatMatchSnapshot(testInfo, projectDir, "src/test/kotlin/com/andy/NativeBonjourResourceIT.kt")
                .satisfies(checkContains("package com.andy"))
                .satisfies(checkContains("class NativeBonjourResourceIT : BonjourResourceTest"));
    }

    @Test
    void generateRESTEasyScalaCustom(TestInfo testInfo) throws Throwable {
        final QuarkusCodestartProjectInput input = QuarkusCodestartProjectInput.builder()
                .addData(getGenerationTestInputData())
                .addExtension(AppArtifactKey.fromString("io.quarkus:quarkus-resteasy"))
                .addExtension(AppArtifactKey.fromString("io.quarkus:quarkus-scala"))
                .putData(PROJECT_PACKAGE_NAME.key(), "com.andy")
                .putData(RESTEASY_EXAMPLE_RESOURCE_CLASS_NAME.key(), "BonjourResource")
                .putData(RESTEASY_EXAMPLE_RESOURCE_PATH.key(), "/bonjour")
                .build();
        final Path projectDir = testDirPath.resolve("resteasy-scala-custom");
        getCatalog().createProject(input).generate(projectDir);

        checkMaven(projectDir);
        checkReadme(projectDir);
        checkDockerfiles(projectDir, BuildTool.MAVEN);
        checkConfigProperties(projectDir);

        assertThatMatchSnapshot(testInfo, projectDir, "src/main/scala/com/andy/BonjourResource.scala")
                .satisfies(checkContains("package com.andy"))
                .satisfies(checkContains("class BonjourResource"))
                .satisfies(checkContains("@Path(\"/bonjour\")"));

        assertThatMatchSnapshot(testInfo, projectDir, "src/test/scala/com/andy/BonjourResourceTest.scala")
                .satisfies(checkContains("package com.andy"))
                .satisfies(checkContains("class BonjourResourceTest"))
                .satisfies(checkContains("\"/bonjour\""));

        assertThatMatchSnapshot(testInfo, projectDir, "src/test/scala/com/andy/NativeBonjourResourceIT.scala")
                .satisfies(checkContains("package com.andy"))
                .satisfies(checkContains("class NativeBonjourResourceIT extends BonjourResourceTest"));
    }

    @Test
    void generateMavenDefaultJava(TestInfo testInfo) throws Throwable {
        final QuarkusCodestartProjectInput input = QuarkusCodestartProjectInput.builder()
                .addData(getGenerationTestInputData())
                .build();
        final Path projectDir = testDirPath.resolve("maven-default-java");
        getCatalog().createProject(input).generate(projectDir);

        checkMaven(projectDir);
        checkReadme(projectDir);
        checkDockerfiles(projectDir, BuildTool.MAVEN);
        checkConfigProperties(projectDir);

        assertThatMatchSnapshot(testInfo, projectDir, "src/main/java/org/acme/GreetingResource.java");
        assertThatMatchSnapshot(testInfo, projectDir, "src/test/java/org/acme/GreetingResourceTest.java");
        assertThatMatchSnapshot(testInfo, projectDir, "src/test/java/org/acme/NativeGreetingResourceIT.java");
    }

    @Test
    void generateMavenResteasyJava(TestInfo testInfo) throws Throwable {
        final QuarkusCodestartProjectInput input = QuarkusCodestartProjectInput.builder()
                .addExtension(AppArtifactKey.fromString("io.quarkus:quarkus-resteasy"))
                .addData(getGenerationTestInputData())
                .build();
        final Path projectDir = testDirPath.resolve("maven-resteasy-java");
        getCatalog().createProject(input).generate(projectDir);

        checkMaven(projectDir);
        checkReadme(projectDir);
        checkDockerfiles(projectDir, BuildTool.MAVEN);
        checkConfigProperties(projectDir);

        assertThatMatchSnapshot(testInfo, projectDir, "src/main/java/org/acme/GreetingResource.java");
        assertThatMatchSnapshot(testInfo, projectDir, "src/test/java/org/acme/GreetingResourceTest.java");
        assertThatMatchSnapshot(testInfo, projectDir, "src/test/java/org/acme/NativeGreetingResourceIT.java");
    }

    @Test
    void generateMavenPicocliJava(TestInfo testInfo) throws Throwable {
        final QuarkusCodestartProjectInput input = QuarkusCodestartProjectInput.builder()
                .addExtension(AppArtifactKey.fromString("io.quarkus:quarkus-picocli"))
                .addData(getGenerationTestInputData())
                .build();
        final Path projectDir = testDirPath.resolve("maven-picocli-java");
        getCatalog().createProject(input).generate(projectDir);

        checkMaven(projectDir);
        checkReadme(projectDir);
        checkDockerfiles(projectDir, BuildTool.MAVEN);
        checkConfigProperties(projectDir);

        assertThatMatchSnapshot(testInfo, projectDir, "src/main/java/org/acme/picocli/EntryCommand.java");
        assertThatMatchSnapshot(testInfo, projectDir, "src/main/java/org/acme/picocli/GoodbyeCommand.java");
        assertThatMatchSnapshot(testInfo, projectDir, "src/main/java/org/acme/picocli/HelloCommand.java");
        assertThatMatchSnapshot(testInfo, projectDir, "src/main/java/org/acme/picocli/GreetingService.java");

        assertThat(projectDir.resolve("README.md"))
                .satisfies(checkContains("./mvnw compile quarkus:dev -Dquarkus.args='hello --first-name=Quarky"));
    }

    @Test
    void generateMavenPicocliKotlin(TestInfo testInfo) throws Throwable {
        final QuarkusCodestartProjectInput input = QuarkusCodestartProjectInput.builder()
                .addExtension(AppArtifactKey.fromString("io.quarkus:quarkus-picocli"))
                .addExtension(AppArtifactKey.fromString("io.quarkus:quarkus-kotlin"))
                .addData(getGenerationTestInputData())
                .build();
        final Path projectDir = testDirPath.resolve("maven-picocli-kotlin");
        getCatalog().createProject(input).generate(projectDir);

        checkMaven(projectDir);
        checkReadme(projectDir);
        checkDockerfiles(projectDir, BuildTool.MAVEN);
        checkConfigProperties(projectDir);

        assertThatMatchSnapshot(testInfo, projectDir, "src/main/kotlin/org/acme/picocli/EntryCommand.kt");
        assertThatMatchSnapshot(testInfo, projectDir, "src/main/kotlin/org/acme/picocli/GoodbyeCommand.kt");
        assertThatMatchSnapshot(testInfo, projectDir, "src/main/kotlin/org/acme/picocli/HelloCommand.kt");
        assertThatMatchSnapshot(testInfo, projectDir, "src/main/kotlin/org/acme/picocli/GreetingService.kt");
    }

    @Test
    void generateMavenPicocliGradle(TestInfo testInfo) throws Throwable {
        final QuarkusCodestartProjectInput input = QuarkusCodestartProjectInput.builder()
                .addExtension(AppArtifactKey.fromString("io.quarkus:quarkus-picocli"))
                .buildTool(BuildTool.GRADLE)
                .addData(getGenerationTestInputData())
                .build();
        final Path projectDir = testDirPath.resolve("maven-picocli-gradle");
        getCatalog().createProject(input).generate(projectDir);

        checkGradle(projectDir);
        checkReadme(projectDir);
        checkDockerfiles(projectDir, BuildTool.GRADLE);
        checkConfigProperties(projectDir);

        assertThat(projectDir.resolve("README.md"))
                .satisfies(checkContains("./gradlew quarkusDev --quarkus-args='hello --first-name=Quarky'"));
    }

    @Test
    void generateMavenConfigYamlJava(TestInfo testInfo) throws Throwable {
        final QuarkusCodestartProjectInput input = QuarkusCodestartProjectInput.builder()
                .addExtension(AppArtifactKey.fromString("io.quarkus:quarkus-config-yaml"))
                .addData(getGenerationTestInputData())
                .build();
        final Path projectDir = testDirPath.resolve("maven-yaml-java");
        getCatalog().createProject(input).generate(projectDir);

        checkMaven(projectDir);
        checkReadme(projectDir);
        checkDockerfiles(projectDir, BuildTool.MAVEN);
        checkConfigYaml(projectDir);

        assertThatMatchSnapshot(testInfo, projectDir, "src/main/java/org/acme/config/ConfigResource.java");
    }

    @Test
    void generateMavenResteasyKotlin(TestInfo testInfo) throws Throwable {
        final QuarkusCodestartProjectInput input = QuarkusCodestartProjectInput.builder()
                .addExtension(AppArtifactKey.fromString("io.quarkus:quarkus-resteasy"))
                .addExtension(AppArtifactKey.fromString("io.quarkus:quarkus-kotlin"))
                .addData(getGenerationTestInputData())
                .build();
        final Path projectDir = testDirPath.resolve("maven-resteasy-kotlin");
        getCatalog().createProject(input).generate(projectDir);

        checkMaven(projectDir);
        checkReadme(projectDir);
        checkDockerfiles(projectDir, BuildTool.MAVEN);
        checkConfigProperties(projectDir);

        assertThatMatchSnapshot(testInfo, projectDir, "src/main/kotlin/org/acme/GreetingResource.kt");
        assertThatMatchSnapshot(testInfo, projectDir, "src/test/kotlin/org/acme/GreetingResourceTest.kt");
        assertThatMatchSnapshot(testInfo, projectDir, "src/test/kotlin/org/acme/NativeGreetingResourceIT.kt");
    }

    @Test
    void generateMavenResteasyScala(TestInfo testInfo) throws Throwable {
        final QuarkusCodestartProjectInput input = QuarkusCodestartProjectInput.builder()
                .addExtension(AppArtifactKey.fromString("io.quarkus:quarkus-resteasy"))
                .addExtension(AppArtifactKey.fromString("io.quarkus:quarkus-scala"))
                .addData(getGenerationTestInputData())
                .build();
        final Path projectDir = testDirPath.resolve("maven-resteasy-scala");
        getCatalog().createProject(input).generate(projectDir);

        checkMaven(projectDir);
        checkReadme(projectDir);
        checkDockerfiles(projectDir, BuildTool.MAVEN);
        checkConfigProperties(projectDir);

        assertThatMatchSnapshot(testInfo, projectDir, "src/main/scala/org/acme/GreetingResource.scala");
        assertThatMatchSnapshot(testInfo, projectDir, "src/test/scala/org/acme/GreetingResourceTest.scala");
        assertThatMatchSnapshot(testInfo, projectDir, "src/test/scala/org/acme/NativeGreetingResourceIT.scala");
    }

    @Test
    void generateGradleResteasyJava(TestInfo testInfo) throws Throwable {
        final QuarkusCodestartProjectInput input = QuarkusCodestartProjectInput.builder()
                .buildTool(BuildTool.GRADLE)
                .addCodestart("gradle")
                .addExtension(AppArtifactKey.fromString("io.quarkus:quarkus-resteasy"))
                .addData(getGenerationTestInputData())
                .build();
        final Path projectDir = testDirPath.resolve("gradle-resteasy-java");
        getCatalog().createProject(input).generate(projectDir);

        checkGradle(projectDir);
        checkReadme(projectDir);
        checkDockerfiles(projectDir, BuildTool.GRADLE);
        checkConfigProperties(projectDir);

        assertThatMatchSnapshot(testInfo, projectDir, "src/main/java/org/acme/GreetingResource.java");
        assertThatMatchSnapshot(testInfo, projectDir, "src/test/java/org/acme/GreetingResourceTest.java");
        assertThatMatchSnapshot(testInfo, projectDir, "src/native-test/java/org/acme/NativeGreetingResourceIT.java");
    }

    @Test
    void generateGradleResteasyKotlin(TestInfo testInfo) throws Throwable {
        final QuarkusCodestartProjectInput input = QuarkusCodestartProjectInput.builder()
                .buildTool(BuildTool.GRADLE)
                .addExtension(AppArtifactKey.fromString("io.quarkus:quarkus-resteasy"))
                .addExtension(AppArtifactKey.fromString("io.quarkus:quarkus-kotlin"))
                .addCodestart("gradle")
                .addData(getGenerationTestInputData())
                .build();
        final Path projectDir = testDirPath.resolve("gradle-resteasy-kotlin");
        getCatalog().createProject(input).generate(projectDir);

        checkGradle(projectDir);
        checkReadme(projectDir);
        checkDockerfiles(projectDir, BuildTool.GRADLE);
        checkConfigProperties(projectDir);

        assertThatMatchSnapshot(testInfo, projectDir, "src/main/kotlin/org/acme/GreetingResource.kt");
        assertThatMatchSnapshot(testInfo, projectDir, "src/test/kotlin/org/acme/GreetingResourceTest.kt");
        assertThatMatchSnapshot(testInfo, projectDir, "src/native-test/kotlin/org/acme/NativeGreetingResourceIT.kt");
    }

    @Test
    void generateGradleResteasyScala(TestInfo testInfo) throws Throwable {
        final QuarkusCodestartProjectInput input = QuarkusCodestartProjectInput.builder()
                .buildTool(BuildTool.GRADLE)
                .addExtension(AppArtifactKey.fromString("io.quarkus:quarkus-resteasy"))
                .addExtension(AppArtifactKey.fromString("io.quarkus:quarkus-scala"))
                .addData(getGenerationTestInputData())
                .build();
        final Path projectDir = testDirPath.resolve("gradle-resteasy-scala");
        getCatalog().createProject(input).generate(projectDir);

        checkGradle(projectDir);
        checkReadme(projectDir);
        checkDockerfiles(projectDir, BuildTool.GRADLE);
        checkConfigProperties(projectDir);

        assertThatMatchSnapshot(testInfo, projectDir, "src/main/scala/org/acme/GreetingResource.scala");
        assertThatMatchSnapshot(testInfo, projectDir, "src/test/scala/org/acme/GreetingResourceTest.scala");
        assertThatMatchSnapshot(testInfo, projectDir, "src/native-test/scala/org/acme/NativeGreetingResourceIT.scala");
    }

    @Test
    void generateGradleWithKotlinDslResteasyJava(TestInfo testInfo) throws Throwable {
        final QuarkusCodestartProjectInput input = QuarkusCodestartProjectInput.builder()
                .buildTool(BuildTool.GRADLE_KOTLIN_DSL)
                .addExtension(AppArtifactKey.fromString("io.quarkus:quarkus-resteasy"))
                .addData(getGenerationTestInputData())
                .build();
        final Path projectDir = testDirPath.resolve("gradle-kotlin-dsl-resteasy-java");
        getCatalog().createProject(input).generate(projectDir);

        checkGradleWithKotlinDsl(projectDir);
        checkReadme(projectDir);
        checkDockerfiles(projectDir, BuildTool.GRADLE_KOTLIN_DSL);
        checkConfigProperties(projectDir);

        assertThatMatchSnapshot(testInfo, projectDir, "src/main/java/org/acme/GreetingResource.java");
    }

    @Test
    void generateGradleWithKotlinDslResteasyKotlin(TestInfo testInfo) throws Throwable {
        final QuarkusCodestartProjectInput input = QuarkusCodestartProjectInput.builder()
                .buildTool(BuildTool.GRADLE_KOTLIN_DSL)
                .addExtension(AppArtifactKey.fromString("io.quarkus:quarkus-resteasy"))
                .addExtension(AppArtifactKey.fromString("io.quarkus:quarkus-kotlin"))
                .addData(getGenerationTestInputData())
                .build();
        final Path projectDir = testDirPath.resolve("gradle-kotlin-dsl-resteasy-kotlin");
        getCatalog().createProject(input).generate(projectDir);

        checkGradleWithKotlinDsl(projectDir);
        checkReadme(projectDir);
        checkDockerfiles(projectDir, BuildTool.GRADLE_KOTLIN_DSL);
        checkConfigProperties(projectDir);

        assertThatMatchSnapshot(testInfo, projectDir, "src/main/kotlin/org/acme/GreetingResource.kt");
    }

    @Test
    void generateGradleWithKotlinDslResteasyScala(TestInfo testInfo) throws Throwable {
        final QuarkusCodestartProjectInput input = QuarkusCodestartProjectInput.builder()
                .buildTool(BuildTool.GRADLE_KOTLIN_DSL)
                .addExtension(AppArtifactKey.fromString("io.quarkus:quarkus-resteasy"))
                .addExtension(AppArtifactKey.fromString("io.quarkus:quarkus-scala"))
                .addData(getGenerationTestInputData())
                .build();
        final Path projectDir = testDirPath.resolve("gradle-kotlin-dsl-resteasy-scala");
        getCatalog().createProject(input).generate(projectDir);

        checkGradleWithKotlinDsl(projectDir);
        checkReadme(projectDir);
        checkDockerfiles(projectDir, BuildTool.GRADLE_KOTLIN_DSL);
        checkConfigProperties(projectDir);

        assertThatMatchSnapshot(testInfo, projectDir, "src/main/scala/org/acme/GreetingResource.scala");
    }

    @Test
    void generateRESTEasyQute(TestInfo testInfo) throws Throwable {
        final QuarkusCodestartProjectInput input = QuarkusCodestartProjectInput.builder()
                .addExtension(AppArtifactKey.fromString("io.quarkus:quarkus-resteasy-qute"))
                .addData(getGenerationTestInputData())
                .build();
        final Path projectDir = testDirPath.resolve("maven-resteasy-qute");
        getCatalog().createProject(input).generate(projectDir);

        checkMaven(projectDir);
        checkReadme(projectDir);
        checkDockerfiles(projectDir, BuildTool.MAVEN);
        checkConfigProperties(projectDir);

        assertThatMatchSnapshot(testInfo, projectDir, "src/main/java/org/acme/resteasyqute/Quark.java");
        assertThatMatchSnapshot(testInfo, projectDir, "src/main/java/org/acme/resteasyqute/QuteResource.java");
    }

    @Test
    void generateWithCustomPackage(TestInfo testInfo) throws Throwable {
        final QuarkusCodestartProjectInput input = QuarkusCodestartProjectInput.builder()
                .addCodestart("resteasy-qute").addCodestart("resteasy").addCodestart("funqy-http")
                .putData(PROJECT_PACKAGE_NAME.key(), "my.custom.app")
                .addData(getGenerationTestInputData())
                .build();
        final Path projectDir = testDirPath.resolve("custom-package");
        getCatalog().createProject(input).generate(projectDir);

        checkMaven(projectDir);
        checkReadme(projectDir);
        checkDockerfiles(projectDir, BuildTool.MAVEN);
        checkConfigProperties(projectDir);

        assertThatMatchSnapshot(testInfo, projectDir, "src/main/java/my/custom/app/GreetingResource.java")
                .satisfies(checkContains("package my.custom.app;"));
        assertThatMatchSnapshot(testInfo, projectDir, "src/test/java/my/custom/app/GreetingResourceTest.java")
                .satisfies(checkContains("package my.custom.app;"));
        assertThatMatchSnapshot(testInfo, projectDir, "src/test/java/my/custom/app/NativeGreetingResourceIT.java")
                .satisfies(checkContains("package my.custom.app;"));
        assertThatMatchSnapshot(testInfo, projectDir, "src/main/java/my/custom/app/resteasyqute/Quark.java")
                .satisfies(checkContains("package my.custom.app.resteasyqute;"));
        assertThatMatchSnapshot(testInfo, projectDir, "src/main/java/my/custom/app/resteasyqute/QuteResource.java")
                .satisfies(checkContains("package my.custom.app.resteasyqute;"));
        assertThatMatchSnapshot(testInfo, projectDir, "src/main/java/my/custom/app/funqy/Funqy.java")
                .satisfies(checkContains("package my.custom.app.funqy;"));
        assertThatMatchSnapshot(testInfo, projectDir, "src/test/java/my/custom/app/funqy/FunqyTest.java")
                .satisfies(checkContains("package my.custom.app.funqy;"));
    }

    @Test
    public void generateGradleWrapperGithubAction(TestInfo testInfo) throws Throwable {
        final QuarkusCodestartProjectInput input = QuarkusCodestartProjectInput.builder()
                .buildTool(BuildTool.GRADLE)
                .addData(getGenerationTestInputData())
                .addCodestarts(Collections.singletonList("github-action"))
                .build();
        Path projectDir = testDirPath.resolve("gradle-github");
        getCatalog().createProject(input).generate(projectDir);

        checkGradle(projectDir);

        assertThatMatchSnapshot(testInfo, projectDir, ".github/workflows/ci.yml")
                .satisfies(checkContains("run: ./gradlew build"));
    }

    @Test
    public void generateGradleNoWrapperGithubAction(TestInfo testInfo) throws Throwable {
        final QuarkusCodestartProjectInput input = QuarkusCodestartProjectInput.builder()
                .buildTool(BuildTool.GRADLE)
                .noBuildToolWrapper()
                .addData(getGenerationTestInputData())
                .addCodestarts(Collections.singletonList("github-action"))
                .build();
        Path projectDir = testDirPath.resolve("gradle-nowrapper-github");
        getCatalog().createProject(input).generate(projectDir);

        checkGradle(projectDir);

        assertThatMatchSnapshot(testInfo, projectDir, ".github/workflows/ci.yml")
                .satisfies(checkContains("uses: eskatos/gradle-command-action@v1"))
                .satisfies(checkContains("arguments: build"));
    }

    private void checkDockerfiles(Path projectDir, BuildTool buildTool) {
        switch (buildTool) {
            case MAVEN:
                checkDockerfilesWithMaven(projectDir);
                break;
            case GRADLE_KOTLIN_DSL:
            case GRADLE:
                checkDockerfilesWithGradle(projectDir);
                break;
            default:
                throw new IllegalArgumentException("Unhandled buildtool");
        }
    }

    private void checkDockerfilesWithMaven(Path projectDir) {
        assertThat(projectDir.resolve(".dockerignore")).exists();
        assertThat(projectDir.resolve("src/main/docker/Dockerfile.jvm")).exists()
                .satisfies(checkContains("./mvnw package"))
                .satisfies(checkContains("docker build -f src/main/docker/Dockerfile.jvm"))
                .satisfies(checkContains("registry.access.redhat.com/ubi8/ubi-minimal:8.3"))
                .satisfies(checkContains("ARG JAVA_PACKAGE=java-11-openjdk-headless"))
                .satisfies(checkContains("ENTRYPOINT [ \"/deployments/run-java.sh\" ]"));
        assertThat(projectDir.resolve("src/main/docker/Dockerfile.legacy-jar")).exists()
                .satisfies(checkContains("./mvnw package -Dquarkus.package.type=legacy-jar"))
                .satisfies(checkContains("docker build -f src/main/docker/Dockerfile.legacy-jar"))
                .satisfies(checkContains("registry.access.redhat.com/ubi8/ubi-minimal:8.3"))
                .satisfies(checkContains("ARG JAVA_PACKAGE=java-11-openjdk-headless"))
                .satisfies(checkContains("ENTRYPOINT [ \"/deployments/run-java.sh\" ]"));
        assertThat(projectDir.resolve("src/main/docker/Dockerfile.native")).exists()
                .satisfies(checkContains("./mvnw package -Pnative"))
                .satisfies(checkContains("registry.access.redhat.com/ubi8/ubi-minimal:8.3"))
                .satisfies(checkContains("CMD [\"./application\", \"-Dquarkus.http.host=0.0.0.0\"]"));
    }

    private void checkDockerfilesWithGradle(Path projectDir) {
        assertThat(projectDir.resolve(".dockerignore")).exists();
        assertThat(projectDir.resolve("src/main/docker/Dockerfile.jvm")).exists()
                .satisfies(checkContains("./gradlew build"))
                .satisfies(checkContains("docker build -f src/main/docker/Dockerfile.jvm"))
                .satisfies(checkContains("registry.access.redhat.com/ubi8/ubi-minimal:8.3"))
                .satisfies(checkContains("ARG JAVA_PACKAGE=java-11-openjdk-headless"))
                .satisfies(checkContains("ENTRYPOINT [ \"/deployments/run-java.sh\" ]"));
        assertThat(projectDir.resolve("src/main/docker/Dockerfile.legacy-jar")).exists()
                .satisfies(checkContains("./gradlew build -Dquarkus.package.type=legacy-jar"))
                .satisfies(checkContains("docker build -f src/main/docker/Dockerfile.legacy-jar"))
                .satisfies(checkContains("registry.access.redhat.com/ubi8/ubi-minimal:8.3"))
                .satisfies(checkContains("ARG JAVA_PACKAGE=java-11-openjdk-headless"))
                .satisfies(checkContains("ENTRYPOINT [ \"/deployments/run-java.sh\" ]"));
        assertThat(projectDir.resolve("src/main/docker/Dockerfile.native")).exists()
                .satisfies(checkContains("./gradlew build -Dquarkus.package.type=native"))
                .satisfies(checkContains("registry.access.redhat.com/ubi8/ubi-minimal:8.3"))
                .satisfies(checkContains("CMD [\"./application\", \"-Dquarkus.http.host=0.0.0.0\"]"));
    }

    private void checkConfigProperties(Path projectDir) {
        assertThat(projectDir.resolve("src/main/resources/application.yml")).doesNotExist();
        assertThat(projectDir.resolve("src/main/resources/application.properties")).exists();
    }

    private void checkConfigYaml(Path projectDir) {
        assertThat(projectDir.resolve("src/main/resources/application.yml")).exists();
        assertThat(projectDir.resolve("src/main/resources/application.properties")).doesNotExist();
    }

    private void checkReadme(Path projectDir) {
        assertThat(projectDir.resolve("README.md")).exists();
        assertThat(projectDir.resolve(".gitignore")).exists();
    }

    private void checkMaven(Path projectDir) {
        assertThat(projectDir.resolve("pom.xml"))
                .exists()
                .satisfies(checkContains("<groupId>org.test</groupId>"))
                .satisfies(checkContains("<artifactId>test-codestart</artifactId>"))
                .satisfies(checkContains("<version>1.0.0-codestart</version>"));
        assertThat(projectDir.resolve("build.gradle")).doesNotExist();
        assertThat(projectDir.resolve("gradle.properties")).doesNotExist();
        assertThat(projectDir.resolve("settings.properties")).doesNotExist();
    }

    private void checkGradle(Path projectDir) {
        assertThat(projectDir.resolve("pom.xml")).doesNotExist();
        assertThat(projectDir.resolve("build.gradle.kts")).doesNotExist();
        assertThat(projectDir.resolve("settings.gradle.kts")).doesNotExist();
        assertThat(projectDir.resolve("build.gradle"))
                .exists()
                .satisfies(checkContains("group 'org.test'"))
                .satisfies(checkContains("version '1.0.0-codestart'"));
        assertThat(projectDir.resolve("gradle.properties")).exists();
        assertThat(projectDir.resolve("settings.gradle"))
                .exists()
                .satisfies(checkContains("rootProject.name='test-codestart'"));
    }

    private void checkGradleWithKotlinDsl(Path projectDir) {
        assertThat(projectDir.resolve("pom.xml")).doesNotExist();
        assertThat(projectDir.resolve("build.gradle")).doesNotExist();
        assertThat(projectDir.resolve("settings.gradle")).doesNotExist();
        assertThat(projectDir.resolve("build.gradle.kts"))
                .exists()
                .satisfies(checkContains("group = \"org.test\""))
                .satisfies(checkContains("version = \"1.0.0-codestart\""));
        assertThat(projectDir.resolve("gradle.properties")).exists();
        assertThat(projectDir.resolve("settings.gradle.kts"))
                .exists()
                .satisfies(checkContains("rootProject.name=\"test-codestart\""));
    }

    private QuarkusCodestartCatalog getCatalog() throws IOException {
        return QuarkusCodestartCatalog.fromExtensionsCatalog(getExtensionsCatalog(), getCodestartsResourceLoaders());
    }

}
