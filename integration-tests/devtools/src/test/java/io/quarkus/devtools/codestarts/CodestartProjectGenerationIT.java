package io.quarkus.devtools.codestarts;

import static io.quarkus.devtools.codestarts.QuarkusCodestartData.DataKey.*;
import static io.quarkus.devtools.codestarts.QuarkusCodestarts.inputBuilder;
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

class CodestartProjectGenerationIT extends PlatformAwareTestBase {

    private static final Path testDirPath = Paths.get("target/codestarts-test");

    @BeforeAll
    static void setUp() throws IOException {
        ProjectTestUtil.delete(testDirPath.toFile());
    }

    private Map<String, Object> getTestInputData() {
        return getTestInputData(null);
    }

    private Map<String, Object> getTestInputData(final Map<String, Object> override) {
        return CodestartProjectGenerationIT.getTestInputData(getPlatformDescriptor(), override);
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
        final CodestartInput input = inputBuilder(getPlatformDescriptor())
                .includeExamples(false)
                .addData(getTestInputData())
                .build();
        final CodestartProject codestartProject = Codestarts.prepareProject(input);
        final Path projectDir = testDirPath.resolve("empty");
        Codestarts.generateProject(codestartProject, projectDir);

        checkMaven(projectDir);
        checkReadme(projectDir);

        assertThat(projectDir.resolve(".mvnw")).doesNotExist();
        assertThat(projectDir.resolve(".dockerignore")).doesNotExist();

        checkNoExample(projectDir);
    }

    @Test
    void generateCodestartProjectEmptyWithExamples() throws IOException {
        final CodestartInput input = inputBuilder(getPlatformDescriptor())
                .addCodestarts(QuarkusCodestarts.getToolingCodestarts(BuildTool.MAVEN, false, false))
                .addData(getTestInputData())
                .build();
        final CodestartProject codestartProject = Codestarts.prepareProject(input);
        final Path projectDir = testDirPath.resolve("empty-examples");
        Codestarts.generateProject(codestartProject, projectDir);

        checkMaven(projectDir);
        checkReadme(projectDir);
        checkDockerfiles(projectDir);
        checkConfigProperties(projectDir);

        assertThat(projectDir.resolve("src/main/java/org/acme/commandmode/GreetingMain.java")).exists();
    }

    @Test
    void generateCodestartProjectMavenResteasyJava() throws IOException {
        final CodestartInput input = inputBuilder(getPlatformDescriptor())
                .addCodestarts(QuarkusCodestarts.getToolingCodestarts(BuildTool.MAVEN, false, false))
                .addExtension(AppArtifactKey.fromString("io.quarkus:quarkus-resteasy"))
                .addData(getTestInputData())
                .build();
        final CodestartProject codestartProject = Codestarts.prepareProject(input);
        final Path projectDir = testDirPath.resolve("maven-resteasy-java");
        Codestarts.generateProject(codestartProject, projectDir);

        checkMaven(projectDir);
        checkReadme(projectDir);
        checkDockerfiles(projectDir);
        checkConfigProperties(projectDir);

        assertThat(projectDir.resolve("src/main/java/org/acme/resteasy/ExampleResource.java")).exists();
    }

    @Test
    void generateCodestartProjectMavenConfigYamlJava() throws IOException {
        final CodestartInput input = inputBuilder(getPlatformDescriptor())
                .addCodestarts(QuarkusCodestarts.getToolingCodestarts(BuildTool.MAVEN, false, false))
                .addExtension(AppArtifactKey.fromString("io.quarkus:quarkus-config-yaml"))
                .addData(getTestInputData())
                .build();
        final CodestartProject codestartProject = Codestarts.prepareProject(input);
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
        final CodestartInput input = inputBuilder(getPlatformDescriptor())
                .addCodestarts(QuarkusCodestarts.getToolingCodestarts(BuildTool.MAVEN, false, false))
                .addExtension(AppArtifactKey.fromString("io.quarkus:quarkus-resteasy"))
                .addExtension(AppArtifactKey.fromString("io.quarkus:quarkus-kotlin"))
                .addData(getTestInputData())
                .build();
        final CodestartProject codestartProject = Codestarts.prepareProject(input);
        final Path projectDir = testDirPath.resolve("maven-resteasy-kotlin");
        Codestarts.generateProject(codestartProject, projectDir);

        checkMaven(projectDir);
        checkReadme(projectDir);
        checkDockerfiles(projectDir);
        checkConfigProperties(projectDir);

        assertThat(projectDir.resolve("src/main/kotlin/org/acme/resteasy/ExampleResource.kt")).exists();
    }

    @Test
    void generateCodestartProjectMavenResteasyScala() throws IOException {
        final CodestartInput input = inputBuilder(getPlatformDescriptor())
                .addCodestarts(QuarkusCodestarts.getToolingCodestarts(BuildTool.MAVEN, false, false))
                .addExtension(AppArtifactKey.fromString("io.quarkus:quarkus-resteasy"))
                .addExtension(AppArtifactKey.fromString("io.quarkus:quarkus-scala"))
                .addData(getTestInputData())
                .build();
        final CodestartProject codestartProject = Codestarts.prepareProject(input);
        final Path projectDir = testDirPath.resolve("maven-resteasy-scala");
        Codestarts.generateProject(codestartProject, projectDir);

        checkMaven(projectDir);
        checkReadme(projectDir);
        checkDockerfiles(projectDir);
        checkConfigProperties(projectDir);

        assertThat(projectDir.resolve("src/main/scala/org/acme/resteasy/ExampleResource.scala")).exists();
    }

    @Test
    void generateCodestartProjectGradleResteasyJava() throws IOException {
        final CodestartInput input = inputBuilder(getPlatformDescriptor())
                .addCodestarts(QuarkusCodestarts.getToolingCodestarts(BuildTool.GRADLE, false, false))
                .addCodestart("gradle")
                .addExtension(AppArtifactKey.fromString("io.quarkus:quarkus-resteasy"))
                .addData(getTestInputData())
                .build();
        final CodestartProject codestartProject = Codestarts.prepareProject(input);
        final Path projectDir = testDirPath.resolve("gradle-resteasy-java");
        Codestarts.generateProject(codestartProject, projectDir);

        checkGradle(projectDir);
        checkReadme(projectDir);
        checkDockerfiles(projectDir);
        checkConfigProperties(projectDir);

        assertThat(projectDir.resolve("src/main/java/org/acme/resteasy/ExampleResource.java")).exists();
    }

    @Test
    void generateCodestartProjectGradleResteasyKotlin() throws IOException {
        final CodestartInput input = inputBuilder(getPlatformDescriptor())
                .addCodestarts(QuarkusCodestarts.getToolingCodestarts(BuildTool.GRADLE, false, false))
                .addExtension(AppArtifactKey.fromString("io.quarkus:quarkus-resteasy"))
                .addExtension(AppArtifactKey.fromString("io.quarkus:quarkus-kotlin"))
                .addCodestart("gradle")
                .addData(getTestInputData())
                .build();
        final CodestartProject codestartProject = Codestarts.prepareProject(input);
        final Path projectDir = testDirPath.resolve("gradle-resteasy-kotlin");
        Codestarts.generateProject(codestartProject, projectDir);

        checkGradle(projectDir);
        checkReadme(projectDir);
        checkDockerfiles(projectDir);
        checkConfigProperties(projectDir);

        assertThat(projectDir.resolve("src/main/kotlin/org/acme/resteasy/ExampleResource.kt")).exists();
    }

    @Test
    void generateCodestartProjectGradleResteasyScala() throws IOException {
        final CodestartInput input = inputBuilder(getPlatformDescriptor())
                .addCodestarts(QuarkusCodestarts.getToolingCodestarts(BuildTool.GRADLE, false, false))
                .addExtension(AppArtifactKey.fromString("io.quarkus:quarkus-resteasy"))
                .addExtension(AppArtifactKey.fromString("io.quarkus:quarkus-scala"))
                .addData(getTestInputData())
                .build();
        final CodestartProject codestartProject = Codestarts.prepareProject(input);
        final Path projectDir = testDirPath.resolve("gradle-resteasy-scala");
        Codestarts.generateProject(codestartProject, projectDir);

        checkGradle(projectDir);
        checkReadme(projectDir);
        checkDockerfiles(projectDir);
        checkConfigProperties(projectDir);

        assertThat(projectDir.resolve("src/main/scala/org/acme/resteasy/ExampleResource.scala")).exists();
    }

    @Test
    void generateCodestartProjectGradleWithKotlinDslResteasyJava() throws IOException {
        final CodestartInput input = inputBuilder(getPlatformDescriptor())
                .addCodestarts(QuarkusCodestarts.getToolingCodestarts(BuildTool.GRADLE_KOTLIN_DSL, false, false))
                .addExtension(AppArtifactKey.fromString("io.quarkus:quarkus-resteasy"))
                .addData(getTestInputData())
                .build();
        final CodestartProject codestartProject = Codestarts.prepareProject(input);
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
        final CodestartInput input = inputBuilder(getPlatformDescriptor())
                .addCodestarts(QuarkusCodestarts.getToolingCodestarts(BuildTool.GRADLE_KOTLIN_DSL, false, false))
                .addExtension(AppArtifactKey.fromString("io.quarkus:quarkus-resteasy"))
                .addExtension(AppArtifactKey.fromString("io.quarkus:quarkus-kotlin"))
                .addData(getTestInputData())
                .build();
        final CodestartProject codestartProject = Codestarts.prepareProject(input);
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
        final CodestartInput input = inputBuilder(getPlatformDescriptor())
                .addCodestarts(QuarkusCodestarts.getToolingCodestarts(BuildTool.GRADLE_KOTLIN_DSL, false, false))
                .addExtension(AppArtifactKey.fromString("io.quarkus:quarkus-resteasy"))
                .addExtension(AppArtifactKey.fromString("io.quarkus:quarkus-scala"))
                .addData(getTestInputData())
                .build();
        final CodestartProject codestartProject = Codestarts.prepareProject(input);
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
        final CodestartInput input = inputBuilder(getPlatformDescriptor())
                .addCodestarts(QuarkusCodestarts.getToolingCodestarts(BuildTool.MAVEN, false, false))
                .addExtension(AppArtifactKey.fromString("io.quarkus:quarkus-qute"))
                .addCodestart("qute")
                .addData(getTestInputData())
                .build();
        final CodestartProject codestartProject = Codestarts.prepareProject(input);
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
