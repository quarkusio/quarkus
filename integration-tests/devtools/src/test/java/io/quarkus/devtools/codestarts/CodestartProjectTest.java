package io.quarkus.devtools.codestarts;

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

class CodestartProjectTest extends PlatformAwareTestBase {

    private static final Path projectPath = Paths.get("target/codestarts-test");

    @BeforeAll
    static void setUp() throws IOException {
        ProjectTestUtil.delete(projectPath.toFile());
    }

    private Map<String, Object> getTestInputData() {
        return getTestInputData(null);
    }

    private Map<String, Object> getTestInputData(final Map<String, Object> override) {
        final HashMap<String, Object> data = new HashMap<>();
        data.put("project.group-id", "org.test");
        data.put("project.artifact-id", "test-codestart");
        data.put("project.version", "1.0.0-codestart");
        data.put("quarkus.platform.group-id", getPlatformDescriptor().getBomGroupId());
        data.put("quarkus.platform.artifact-id", getPlatformDescriptor().getBomArtifactId());
        data.put("quarkus.platform.version", getPlatformDescriptor().getBomVersion());
        data.put("quarkus.version", getPlatformDescriptor().getQuarkusVersion());
        data.put("quarkus.plugin.group-id", "io.quarkus");
        data.put("quarkus.plugin.artifact-id", "quarkus-maven-plugin");
        data.put("quarkus.plugin.version", getPlatformDescriptor().getQuarkusVersion());
        data.put("java.version", "11");
        if (override != null)
            data.putAll(override);
        return data;
    }

    @Test
    void generateCodestartProjectEmpty() throws IOException {
        final CodestartInput input = inputBuilder(getPlatformDescriptor())
                .addData(getTestInputData())
                .build();
        final CodestartProject codestartProject = Codestarts.prepareProject(input);
        final Path projectDir = projectPath.resolve("empty");
        Codestarts.generateProject(codestartProject, projectDir);

        checkMaven(projectDir);
        checkReadme(projectDir);
        checkDockerfiles(projectDir);
        checkNoExample(projectDir);
    }

    @Test
    void generateCodestartProjectEmptyWithExamples() throws IOException {
        final CodestartInput input = inputBuilder(getPlatformDescriptor())
                .includeExamples()
                .addData(getTestInputData())
                .build();
        final CodestartProject codestartProject = Codestarts.prepareProject(input);
        final Path projectDir = projectPath.resolve("empty-examples");
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
                .includeExamples()
                .addExtension(AppArtifactKey.fromString("io.quarkus:quarkus-resteasy"))
                .addData(getTestInputData())
                .build();
        final CodestartProject codestartProject = Codestarts.prepareProject(input);
        final Path projectDir = projectPath.resolve("maven-resteasy-java");
        Codestarts.generateProject(codestartProject, projectDir);

        checkMaven(projectDir);
        checkReadme(projectDir);
        checkDockerfiles(projectDir);
        checkConfigProperties(projectDir);

        assertThat(projectDir.resolve("src/main/java/org/acme/resteasy/ExampleResource.java")).exists();
    }

    @Test
    void generateCodestartProjectMavenResteasyKotlin() throws IOException {
        final CodestartInput input = inputBuilder(getPlatformDescriptor())
                .includeExamples()
                .addExtension(AppArtifactKey.fromString("io.quarkus:quarkus-resteasy"))
                .addExtension(AppArtifactKey.fromString("io.quarkus:quarkus-kotlin"))
                .addData(getTestInputData())
                .build();
        final CodestartProject codestartProject = Codestarts.prepareProject(input);
        final Path projectDir = projectPath.resolve("maven-resteasy-kotlin");
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
                .includeExamples()
                .addExtension(AppArtifactKey.fromString("io.quarkus:quarkus-resteasy"))
                .addExtension(AppArtifactKey.fromString("io.quarkus:quarkus-scala"))
                .addData(getTestInputData())
                .build();
        final CodestartProject codestartProject = Codestarts.prepareProject(input);
        final Path projectDir = projectPath.resolve("maven-resteasy-scala");
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
                .includeExamples()
                .addExtension(AppArtifactKey.fromString("io.quarkus:quarkus-resteasy"))
                .addData(getTestInputData())
                .putData("buildtool.name", "gradle")
                .build();
        final CodestartProject codestartProject = Codestarts.prepareProject(input);
        final Path projectDir = projectPath.resolve("gradle-resteasy-java");
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
                .includeExamples()
                .addExtension(AppArtifactKey.fromString("io.quarkus:quarkus-resteasy"))
                .addExtension(AppArtifactKey.fromString("io.quarkus:quarkus-kotlin"))
                .putData("buildtool.name", "gradle")
                .addData(getTestInputData())
                .build();
        final CodestartProject codestartProject = Codestarts.prepareProject(input);
        final Path projectDir = projectPath.resolve("gradle-resteasy-kotlin");
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
                .includeExamples()
                .addExtension(AppArtifactKey.fromString("io.quarkus:quarkus-resteasy"))
                .addExtension(AppArtifactKey.fromString("io.quarkus:quarkus-scala"))
                .addData(getTestInputData())
                .putData("buildtool.name", "gradle")
                .build();
        final CodestartProject codestartProject = Codestarts.prepareProject(input);
        final Path projectDir = projectPath.resolve("gradle-resteasy-scala");
        Codestarts.generateProject(codestartProject, projectDir);

        checkGradle(projectDir);
        checkReadme(projectDir);
        checkDockerfiles(projectDir);
        checkConfigProperties(projectDir);

        assertThat(projectDir.resolve("src/main/scala/org/acme/resteasy/ExampleResource.scala")).exists();
    }

    @Test
    void generateCodestartProjectQute() throws IOException {
        final CodestartInput input = inputBuilder(getPlatformDescriptor())
                .includeExamples()
                .addExtension(AppArtifactKey.fromString("io.quarkus:quarkus-qute"))
                .addCodestart("qute")
                .addData(getTestInputData())
                .build();
        final CodestartProject codestartProject = Codestarts.prepareProject(input);
        final Path projectDir = projectPath.resolve("maven-qute");
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
        assertThat(projectDir.resolve("build.gradle"))
                .exists()
                .satisfies(checkContains("group 'org.test'"))
                .satisfies(checkContains("version '1.0.0-codestart'"));
        assertThat(projectDir.resolve("gradle.properties")).exists();
        assertThat(projectDir.resolve("settings.gradle"))
                .exists()
                .satisfies(checkContains("rootProject.name='test-codestart'"));
    }

    private Consumer<Path> checkContains(String s) {
        return (p) -> assertThat(Files.contentOf(p.toFile(), StandardCharsets.UTF_8)).contains(s);
    }

}
