package io.quarkus.devtools.codestarts;

import static io.quarkus.devtools.codestarts.QuarkusCodestartData.DataKey.*;
import static io.quarkus.devtools.codestarts.QuarkusCodestarts.prepareProject;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.assertj.core.util.Files;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.quarkus.bootstrap.model.AppArtifactKey;
import io.quarkus.devtools.PlatformAwareTestBase;
import io.quarkus.devtools.ProjectTestUtil;
import io.quarkus.devtools.project.BuildTool;
import io.quarkus.platform.descriptor.QuarkusPlatformDescriptor;

class QuarkusCodestartGenerateTest extends PlatformAwareTestBase {

    private static final Path testDirPath = Paths.get("target/codestarts-test");

    @BeforeAll
    static void setUp() throws IOException {
        ProjectTestUtil.delete(testDirPath.toFile());
    }

    private Map<String, Object> getTestInputData() {
        return getTestInputData(null);
    }

    private Map<String, Object> getTestInputData(final Map<String, Object> override) {
        return QuarkusCodestartGenerateTest.getTestInputData(getPlatformDescriptor(), override);
    }

    static Map<String, Object> getTestInputData(final QuarkusPlatformDescriptor descriptor,
            final Map<String, Object> override) {
        final HashMap<String, Object> data = new HashMap<>();
        data.put(PROJECT_GROUP_ID.getKey(), "org.test");
        data.put(PROJECT_ARTIFACT_ID.getKey(), "test-codestart");
        data.put(PROJECT_VERSION.getKey(), "1.0.0-codestart");
        data.put(BOM_GROUP_ID.getKey(), descriptor.getBomGroupId());
        data.put(BOM_ARTIFACT_ID.getKey(), descriptor.getBomArtifactId());
        data.put(BOM_VERSION.getKey(), descriptor.getBomVersion());
        data.put(QUARKUS_VERSION.getKey(), descriptor.getQuarkusVersion());
        data.put(QUARKUS_MAVEN_PLUGIN_GROUP_ID.getKey(), "io.quarkus");
        data.put(QUARKUS_MAVEN_PLUGIN_ARTIFACT_ID.getKey(), "quarkus-maven-plugin");
        data.put(QUARKUS_MAVEN_PLUGIN_VERSION.getKey(), descriptor.getQuarkusVersion());
        data.put(QUARKUS_GRADLE_PLUGIN_ID.getKey(), "io.quarkus");
        data.put(QUARKUS_GRADLE_PLUGIN_VERSION.getKey(), descriptor.getQuarkusVersion());
        data.put(JAVA_VERSION.getKey(), "11");
        if (override != null)
            data.putAll(override);
        return data;
    }

    @Test
    void generateCodestartProjectEmpty() throws IOException {
        final QuarkusCodestartInput input = QuarkusCodestartInput.builder(getPlatformDescriptor())
                .noExamples()
                .noDockerfiles()
                .noBuildToolWrapper()
                .addData(getTestInputData())
                .build();
        final CodestartProject codestartProject = prepareProject(input);
        final Path projectDir = testDirPath.resolve("empty");
        Codestarts.generateProject(codestartProject, projectDir);

        checkMaven(projectDir);
        checkReadme(projectDir);

        assertThat(projectDir.resolve(".mvnw")).doesNotExist();
        assertThat(projectDir.resolve(".dockerignore")).doesNotExist();

        checkNoExample(projectDir);
    }

    @Test
    void generateCodestartProjectDefaultWithExamples() throws IOException {
        final QuarkusCodestartInput input = QuarkusCodestartInput.builder(getPlatformDescriptor())
                .addData(getTestInputData())
                .build();
        final CodestartProject codestartProject = prepareProject(input);
        final Path projectDir = testDirPath.resolve("default-examples");
        Codestarts.generateProject(codestartProject, projectDir);

        checkMaven(projectDir);
        checkReadme(projectDir);
        checkDockerfiles(projectDir);
        checkConfigProperties(projectDir);

        assertThat(projectDir.resolve("src/main/java/org/acme/commandmode/HelloCommando.java")).exists();
    }

    @Test
    void generateCodestartProjectCommandModeCustom() throws IOException {
        final QuarkusCodestartInput input = QuarkusCodestartInput.builder(getPlatformDescriptor())
                .addData(getTestInputData())
                .putData(COMMANDMODE_EXAMPLE_PACKAGE_NAME.getKey(), "com.test.andy")
                .putData(COMMANDMODE_EXAMPLE_RESOURCE_CLASS_NAME.getKey(), "AndyCommando")
                .build();
        final CodestartProject codestartProject = prepareProject(input);
        final Path projectDir = testDirPath.resolve("commandmode-custom");
        Codestarts.generateProject(codestartProject, projectDir);

        checkMaven(projectDir);
        checkReadme(projectDir);
        checkDockerfiles(projectDir);
        checkConfigProperties(projectDir);

        assertThat(projectDir.resolve("src/main/java/com/test/andy/AndyCommando.java")).exists()
                .satisfies(checkContains("package com.test.andy;"))
                .satisfies(checkContains("class AndyCommando"));
    }

    @Test
    void generateCodestartProjectRESTEasyJavaCustom() throws IOException {
        final QuarkusCodestartInput input = QuarkusCodestartInput.builder(getPlatformDescriptor())
                .addData(getTestInputData())
                .addExtension(AppArtifactKey.fromString("io.quarkus:quarkus-resteasy"))
                .putData(RESTEASY_EXAMPLE_PACKAGE_NAME.getKey(), "com.andy")
                .putData(RESTEASY_EXAMPLE_RESOURCE_CLASS_NAME.getKey(), "BonjourResource")
                .putData(RESTEASY_EXAMPLE_RESOURCE_PATH.getKey(), "/bonjour")
                .build();
        final CodestartProject codestartProject = prepareProject(input);
        final Path projectDir = testDirPath.resolve("resteasy-java-custom");
        Codestarts.generateProject(codestartProject, projectDir);

        checkMaven(projectDir);
        checkReadme(projectDir);
        checkDockerfiles(projectDir);
        checkConfigProperties(projectDir);

        assertThat(projectDir.resolve("src/main/java/com/andy/BonjourResource.java")).exists()
                .satisfies(checkContains("package com.andy;"))
                .satisfies(checkContains("class BonjourResource"))
                .satisfies(checkContains("@Path(\"/bonjour\")"));

        assertThat(projectDir.resolve("src/test/java/com/andy/BonjourResourceTest.java")).exists()
                .satisfies(checkContains("package com.andy;"))
                .satisfies(checkContains("class BonjourResourceTest"))
                .satisfies(checkContains("\"/bonjour\""));

        assertThat(projectDir.resolve("src/test/java/com/andy/NativeBonjourResourceIT.java")).exists()
                .satisfies(checkContains("package com.andy;"))
                .satisfies(checkContains("class NativeBonjourResourceIT extends BonjourResourceTest"));
    }

    @Test
    void generateCodestartProjectRESTEasyKotlinCustom() throws IOException {
        final QuarkusCodestartInput input = QuarkusCodestartInput.builder(getPlatformDescriptor())
                .addData(getTestInputData())
                .addExtension(AppArtifactKey.fromString("io.quarkus:quarkus-resteasy"))
                .addExtension(AppArtifactKey.fromString("io.quarkus:quarkus-kotlin"))
                .putData(RESTEASY_EXAMPLE_PACKAGE_NAME.getKey(), "com.andy")
                .putData(RESTEASY_EXAMPLE_RESOURCE_CLASS_NAME.getKey(), "BonjourResource")
                .putData(RESTEASY_EXAMPLE_RESOURCE_PATH.getKey(), "/bonjour")
                .build();
        final CodestartProject codestartProject = prepareProject(input);
        final Path projectDir = testDirPath.resolve("resteasy-kotlin-custom");
        Codestarts.generateProject(codestartProject, projectDir);

        checkMaven(projectDir);
        checkReadme(projectDir);
        checkDockerfiles(projectDir);
        checkConfigProperties(projectDir);

        assertThat(projectDir.resolve("src/main/kotlin/com/andy/BonjourResource.kt")).exists()
                .satisfies(checkContains("package com.andy"))
                .satisfies(checkContains("class BonjourResource"))
                .satisfies(checkContains("@Path(\"/bonjour\")"));

        assertThat(projectDir.resolve("src/test/kotlin/com/andy/BonjourResourceTest.kt")).exists()
                .satisfies(checkContains("package com.andy"))
                .satisfies(checkContains("class BonjourResourceTest"))
                .satisfies(checkContains("\"/bonjour\""));

        assertThat(projectDir.resolve("src/test/kotlin/com/andy/NativeBonjourResourceIT.kt")).exists()
                .satisfies(checkContains("package com.andy"))
                .satisfies(checkContains("class NativeBonjourResourceIT : BonjourResourceTest"));
    }

    @Test
    void generateCodestartProjectRESTEasyScalaCustom() throws IOException {
        final QuarkusCodestartInput input = QuarkusCodestartInput.builder(getPlatformDescriptor())
                .addData(getTestInputData())
                .addExtension(AppArtifactKey.fromString("io.quarkus:quarkus-resteasy"))
                .addExtension(AppArtifactKey.fromString("io.quarkus:quarkus-scala"))
                .putData(RESTEASY_EXAMPLE_PACKAGE_NAME.getKey(), "com.andy")
                .putData(RESTEASY_EXAMPLE_RESOURCE_CLASS_NAME.getKey(), "BonjourResource")
                .putData(RESTEASY_EXAMPLE_RESOURCE_PATH.getKey(), "/bonjour")
                .build();
        final CodestartProject codestartProject = prepareProject(input);
        final Path projectDir = testDirPath.resolve("resteasy-scala-custom");
        Codestarts.generateProject(codestartProject, projectDir);

        checkMaven(projectDir);
        checkReadme(projectDir);
        checkDockerfiles(projectDir);
        checkConfigProperties(projectDir);

        assertThat(projectDir.resolve("src/main/scala/com/andy/BonjourResource.scala")).exists()
                .satisfies(checkContains("package com.andy"))
                .satisfies(checkContains("class BonjourResource"))
                .satisfies(checkContains("@Path(\"/bonjour\")"));

        assertThat(projectDir.resolve("src/test/scala/com/andy/BonjourResourceTest.scala")).exists()
                .satisfies(checkContains("package com.andy"))
                .satisfies(checkContains("class BonjourResourceTest"))
                .satisfies(checkContains("\"/bonjour\""));

        assertThat(projectDir.resolve("src/test/scala/com/andy/NativeBonjourResourceIT.scala")).exists()
                .satisfies(checkContains("package com.andy"))
                .satisfies(checkContains("class NativeBonjourResourceIT extends BonjourResourceTest"));
    }

    @Test
    void generateCodestartProjectMavenResteasyJava() throws IOException {
        final QuarkusCodestartInput input = QuarkusCodestartInput.builder(getPlatformDescriptor())
                .addExtension(AppArtifactKey.fromString("io.quarkus:quarkus-resteasy"))
                .addData(getTestInputData())
                .build();
        final CodestartProject codestartProject = prepareProject(input);
        final Path projectDir = testDirPath.resolve("maven-resteasy-java");
        Codestarts.generateProject(codestartProject, projectDir);

        checkMaven(projectDir);
        checkReadme(projectDir);
        checkDockerfiles(projectDir);
        checkConfigProperties(projectDir);

        assertThat(projectDir.resolve("src/main/java/org/acme/resteasy/ExampleResource.java")).exists();
        assertThat(projectDir.resolve("src/test/java/org/acme/resteasy/ExampleResourceTest.java")).exists();
        assertThat(projectDir.resolve("src/test/java/org/acme/resteasy/NativeExampleResourceIT.java")).exists();
    }

    @Test
    void generateCodestartProjectMavenConfigYamlJava() throws IOException {
        final QuarkusCodestartInput input = QuarkusCodestartInput.builder(getPlatformDescriptor())
                .addExtension(AppArtifactKey.fromString("io.quarkus:quarkus-config-yaml"))
                .addData(getTestInputData())
                .build();
        final CodestartProject codestartProject = prepareProject(input);
        final Path projectDir = testDirPath.resolve("maven-yaml-java");
        Codestarts.generateProject(codestartProject, projectDir);

        checkMaven(projectDir);
        checkReadme(projectDir);
        checkDockerfiles(projectDir);
        checkConfigYaml(projectDir);

        assertThat(projectDir.resolve("src/main/java/org/acme/config/GreetingResource.java")).exists();
    }

    @Test
    void generateCodestartProjectMavenResteasyKotlin() throws IOException {
        final QuarkusCodestartInput input = QuarkusCodestartInput.builder(getPlatformDescriptor())
                .addExtension(AppArtifactKey.fromString("io.quarkus:quarkus-resteasy"))
                .addExtension(AppArtifactKey.fromString("io.quarkus:quarkus-kotlin"))
                .addData(getTestInputData())
                .build();
        final CodestartProject codestartProject = prepareProject(input);
        final Path projectDir = testDirPath.resolve("maven-resteasy-kotlin");
        Codestarts.generateProject(codestartProject, projectDir);

        checkMaven(projectDir);
        checkReadme(projectDir);
        checkDockerfiles(projectDir);
        checkConfigProperties(projectDir);

        assertThat(projectDir.resolve("src/main/kotlin/org/acme/resteasy/ExampleResource.kt")).exists();
        assertThat(projectDir.resolve("src/test/kotlin/org/acme/resteasy/ExampleResourceTest.kt")).exists();
        assertThat(projectDir.resolve("src/test/kotlin/org/acme/resteasy/NativeExampleResourceIT.kt")).exists();
    }

    @Test
    void generateCodestartProjectMavenResteasyScala() throws IOException {
        final QuarkusCodestartInput input = QuarkusCodestartInput.builder(getPlatformDescriptor())
                .addExtension(AppArtifactKey.fromString("io.quarkus:quarkus-resteasy"))
                .addExtension(AppArtifactKey.fromString("io.quarkus:quarkus-scala"))
                .addData(getTestInputData())
                .build();
        final CodestartProject codestartProject = prepareProject(input);
        final Path projectDir = testDirPath.resolve("maven-resteasy-scala");
        Codestarts.generateProject(codestartProject, projectDir);

        checkMaven(projectDir);
        checkReadme(projectDir);
        checkDockerfiles(projectDir);
        checkConfigProperties(projectDir);

        assertThat(projectDir.resolve("src/main/scala/org/acme/resteasy/ExampleResource.scala")).exists();
        assertThat(projectDir.resolve("src/test/scala/org/acme/resteasy/ExampleResourceTest.scala")).exists();
        assertThat(projectDir.resolve("src/test/scala/org/acme/resteasy/NativeExampleResourceIT.scala")).exists();
    }

    @Test
    void generateCodestartProjectGradleResteasyJava() throws IOException {
        final QuarkusCodestartInput input = QuarkusCodestartInput.builder(getPlatformDescriptor())
                .buildTool(BuildTool.GRADLE)
                .addCodestart("gradle")
                .addExtension(AppArtifactKey.fromString("io.quarkus:quarkus-resteasy"))
                .addData(getTestInputData())
                .build();
        final CodestartProject codestartProject = prepareProject(input);
        final Path projectDir = testDirPath.resolve("gradle-resteasy-java");
        Codestarts.generateProject(codestartProject, projectDir);

        checkGradle(projectDir);
        checkReadme(projectDir);
        checkDockerfiles(projectDir);
        checkConfigProperties(projectDir);

        assertThat(projectDir.resolve("src/main/java/org/acme/resteasy/ExampleResource.java")).exists();
        assertThat(projectDir.resolve("src/test/java/org/acme/resteasy/ExampleResourceTest.java")).exists();
        assertThat(projectDir.resolve("src/native-test/java/org/acme/resteasy/NativeExampleResourceIT.java")).exists();
    }

    @Test
    void generateCodestartProjectGradleResteasyKotlin() throws IOException {
        final QuarkusCodestartInput input = QuarkusCodestartInput.builder(getPlatformDescriptor())
                .buildTool(BuildTool.GRADLE)
                .addExtension(AppArtifactKey.fromString("io.quarkus:quarkus-resteasy"))
                .addExtension(AppArtifactKey.fromString("io.quarkus:quarkus-kotlin"))
                .addCodestart("gradle")
                .addData(getTestInputData())
                .build();
        final CodestartProject codestartProject = prepareProject(input);
        final Path projectDir = testDirPath.resolve("gradle-resteasy-kotlin");
        Codestarts.generateProject(codestartProject, projectDir);

        checkGradle(projectDir);
        checkReadme(projectDir);
        checkDockerfiles(projectDir);
        checkConfigProperties(projectDir);

        assertThat(projectDir.resolve("src/main/kotlin/org/acme/resteasy/ExampleResource.kt")).exists();
        assertThat(projectDir.resolve("src/test/kotlin/org/acme/resteasy/ExampleResourceTest.kt")).exists();
        assertThat(projectDir.resolve("src/native-test/kotlin/org/acme/resteasy/NativeExampleResourceIT.kt")).exists();
    }

    @Test
    void generateCodestartProjectGradleResteasyScala() throws IOException {
        final QuarkusCodestartInput input = QuarkusCodestartInput.builder(getPlatformDescriptor())
                .buildTool(BuildTool.GRADLE)
                .addExtension(AppArtifactKey.fromString("io.quarkus:quarkus-resteasy"))
                .addExtension(AppArtifactKey.fromString("io.quarkus:quarkus-scala"))
                .addData(getTestInputData())
                .build();
        final CodestartProject codestartProject = prepareProject(input);
        final Path projectDir = testDirPath.resolve("gradle-resteasy-scala");
        Codestarts.generateProject(codestartProject, projectDir);

        checkGradle(projectDir);
        checkReadme(projectDir);
        checkDockerfiles(projectDir);
        checkConfigProperties(projectDir);

        assertThat(projectDir.resolve("src/main/scala/org/acme/resteasy/ExampleResource.scala")).exists();
        assertThat(projectDir.resolve("src/test/scala/org/acme/resteasy/ExampleResourceTest.scala")).exists();
        assertThat(projectDir.resolve("src/native-test/scala/org/acme/resteasy/NativeExampleResourceIT.scala")).exists();
    }

    @Test
    void generateCodestartProjectGradleWithKotlinDslResteasyJava() throws IOException {
        final QuarkusCodestartInput input = QuarkusCodestartInput.builder(getPlatformDescriptor())
                .buildTool(BuildTool.GRADLE_KOTLIN_DSL)
                .addExtension(AppArtifactKey.fromString("io.quarkus:quarkus-resteasy"))
                .addData(getTestInputData())
                .build();
        final CodestartProject codestartProject = prepareProject(input);
        final Path projectDir = testDirPath.resolve("gradle-kotlin-dsl-resteasy-java");
        Codestarts.generateProject(codestartProject, projectDir);

        checkGradleWithKotlinDsl(projectDir);
        checkReadme(projectDir);
        checkDockerfiles(projectDir);
        checkConfigProperties(projectDir);

        assertThat(projectDir.resolve("src/main/java/org/acme/resteasy/ExampleResource.java")).exists();
    }

    @Test
    void generateCodestartProjectGradleWithKotlinDslResteasyKotlin() throws IOException {
        final QuarkusCodestartInput input = QuarkusCodestartInput.builder(getPlatformDescriptor())
                .buildTool(BuildTool.GRADLE_KOTLIN_DSL)
                .addExtension(AppArtifactKey.fromString("io.quarkus:quarkus-resteasy"))
                .addExtension(AppArtifactKey.fromString("io.quarkus:quarkus-kotlin"))
                .addData(getTestInputData())
                .build();
        final CodestartProject codestartProject = prepareProject(input);
        final Path projectDir = testDirPath.resolve("gradle-kotlin-dsl-resteasy-kotlin");
        Codestarts.generateProject(codestartProject, projectDir);

        checkGradleWithKotlinDsl(projectDir);
        checkReadme(projectDir);
        checkDockerfiles(projectDir);
        checkConfigProperties(projectDir);

        assertThat(projectDir.resolve("src/main/kotlin/org/acme/resteasy/ExampleResource.kt")).exists();
    }

    @Test
    void generateCodestartProjectGradleWithKotlinDslResteasyScala() throws IOException {
        final QuarkusCodestartInput input = QuarkusCodestartInput.builder(getPlatformDescriptor())
                .buildTool(BuildTool.GRADLE_KOTLIN_DSL)
                .addExtension(AppArtifactKey.fromString("io.quarkus:quarkus-resteasy"))
                .addExtension(AppArtifactKey.fromString("io.quarkus:quarkus-scala"))
                .addData(getTestInputData())
                .build();
        final CodestartProject codestartProject = prepareProject(input);
        final Path projectDir = testDirPath.resolve("gradle-kotlin-dsl-resteasy-scala");
        Codestarts.generateProject(codestartProject, projectDir);

        checkGradleWithKotlinDsl(projectDir);
        checkReadme(projectDir);
        checkDockerfiles(projectDir);
        checkConfigProperties(projectDir);

        assertThat(projectDir.resolve("src/main/scala/org/acme/resteasy/ExampleResource.scala")).exists();
    }

    @Test
    void generateCodestartProjectQute() throws IOException {
        final QuarkusCodestartInput input = QuarkusCodestartInput.builder(getPlatformDescriptor())
                .addExtension(AppArtifactKey.fromString("io.quarkus:quarkus-qute"))
                .addCodestart("qute")
                .addData(getTestInputData())
                .build();
        final CodestartProject codestartProject = prepareProject(input);
        final Path projectDir = testDirPath.resolve("maven-qute");
        Codestarts.generateProject(codestartProject, projectDir);

        checkMaven(projectDir);
        checkReadme(projectDir);
        checkDockerfiles(projectDir);
        checkConfigProperties(projectDir);

        assertThat(projectDir.resolve("src/main/java/org/acme/qute/Item.java")).exists();
    }

    private void checkNoExample(Path projectDir) {
        assertThat(projectDir.resolve("src/main/java")).doesNotExist();
        assertThat(projectDir.resolve("src/main/kotlin")).doesNotExist();
        assertThat(projectDir.resolve("src/main/scala")).doesNotExist();
    }

    private void checkDockerfiles(Path projectDir) {
        assertThat(projectDir.resolve(".dockerignore")).exists();
        assertThat(projectDir.resolve("src/main/docker/Dockerfile.jvm")).exists();
        assertThat(projectDir.resolve("src/main/docker/Dockerfile.native")).exists();
        assertThat(projectDir.resolve("src/main/docker/Dockerfile.fast-jar")).exists();
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

    private Consumer<Path> checkContains(String s) {
        return (p) -> assertThat(Files.contentOf(p.toFile(), StandardCharsets.UTF_8)).contains(s);
    }

}
