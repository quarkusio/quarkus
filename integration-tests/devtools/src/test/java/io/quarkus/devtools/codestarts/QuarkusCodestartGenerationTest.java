package io.quarkus.devtools.codestarts;

import static io.quarkus.devtools.ProjectTestUtil.checkContains;
import static io.quarkus.devtools.codestarts.QuarkusCodestartData.DataKey.*;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.quarkus.bootstrap.model.AppArtifactCoords;
import io.quarkus.bootstrap.model.AppArtifactKey;
import io.quarkus.devtools.PlatformAwareTestBase;
import io.quarkus.devtools.ProjectTestUtil;
import io.quarkus.devtools.project.BuildTool;
import io.quarkus.platform.descriptor.QuarkusPlatformDescriptor;

class QuarkusCodestartGenerationTest extends PlatformAwareTestBase {

    private static final Path testDirPath = Paths.get("target/codestarts-test");

    @BeforeAll
    static void setUp() throws IOException {
        ProjectTestUtil.delete(testDirPath.toFile());
    }

    private Map<String, Object> getTestInputData() {
        return getTestInputData(null);
    }

    private Map<String, Object> getTestInputData(final Map<String, Object> override) {
        return QuarkusCodestartGenerationTest.getTestInputData(getPlatformDescriptor(), override);
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
        final QuarkusCodestartProjectInput input = QuarkusCodestartProjectInput.builder()
                .noExamples()
                .noDockerfiles()
                .noBuildToolWrapper()
                .addData(getTestInputData())
                .build();
        final Path projectDir = testDirPath.resolve("empty");
        getCatalog().createProject(input).generate(projectDir);

        checkMaven(projectDir);
        checkReadme(projectDir);

        assertThat(projectDir.resolve(".mvnw")).doesNotExist();
        assertThat(projectDir.resolve(".dockerignore")).doesNotExist();

        checkNoExample(projectDir);
    }

    @Test
    void generateCodestartProjectCommandMode() throws IOException {
        final QuarkusCodestartProjectInput input = QuarkusCodestartProjectInput.builder()
                .addCodestart("commandmode")
                .addData(getTestInputData())
                .build();

        final Path projectDir = testDirPath.resolve("default-examples");
        getCatalog().createProject(input).generate(projectDir);

        checkMaven(projectDir);
        checkReadme(projectDir);
        checkDockerfiles(projectDir, BuildTool.MAVEN);
        checkConfigProperties(projectDir);

        assertThat(projectDir.resolve("src/main/java/org/acme/commandmode/HelloCommando.java")).exists();
    }

    @Test
    void generateCodestartProjectCommandModeCustom() throws IOException {
        final QuarkusCodestartProjectInput input = QuarkusCodestartProjectInput.builder()
                .addCodestart("commandmode")
                .addData(getTestInputData())
                .putData(COMMANDMODE_EXAMPLE_PACKAGE_NAME.getKey(), "com.test.andy")
                .putData(COMMANDMODE_EXAMPLE_RESOURCE_CLASS_NAME.getKey(), "AndyCommando")
                .build();
        final Path projectDir = testDirPath.resolve("commandmode-custom");
        getCatalog().createProject(input).generate(projectDir);

        checkMaven(projectDir);
        checkReadme(projectDir);
        checkDockerfiles(projectDir, BuildTool.MAVEN);
        checkConfigProperties(projectDir);

        assertThat(projectDir.resolve("src/main/java/com/test/andy/AndyCommando.java")).exists()
                .satisfies(checkContains("package com.test.andy;"))
                .satisfies(checkContains("class AndyCommando"));
    }

    @Test
    void generateCodestartProjectRESTEasyJavaCustom() throws IOException {
        final QuarkusCodestartProjectInput input = QuarkusCodestartProjectInput.builder()
                .addData(getTestInputData())
                .addExtension(AppArtifactKey.fromString("io.quarkus:quarkus-resteasy"))
                .putData(RESTEASY_EXAMPLE_PACKAGE_NAME.getKey(), "com.andy")
                .putData(RESTEASY_EXAMPLE_RESOURCE_CLASS_NAME.getKey(), "BonjourResource")
                .putData(RESTEASY_EXAMPLE_RESOURCE_PATH.getKey(), "/bonjour")
                .build();
        final Path projectDir = testDirPath.resolve("resteasy-java-custom");
        getCatalog().createProject(input).generate(projectDir);

        checkMaven(projectDir);
        checkReadme(projectDir);
        checkDockerfiles(projectDir, BuildTool.MAVEN);
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
    void generateMavenProjectWithCustomDep() throws IOException {
        final QuarkusCodestartProjectInput input = QuarkusCodestartProjectInput.builder()
                .addData(getTestInputData())
                .addExtension(AppArtifactKey.fromString("io.quarkus:quarkus-resteasy"))
                .addExtension(AppArtifactCoords.fromString("commons-io:commons-io:2.5"))

                .build();
        final Path projectDir = testDirPath.resolve("maven-custom-dep");
        getCatalog().createProject(input).generate(projectDir);

        checkMaven(projectDir);
        assertThat(projectDir.resolve("pom.xml")).exists()
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
    void generateCodestartProjectRESTEasyKotlinCustom() throws IOException {
        final QuarkusCodestartProjectInput input = QuarkusCodestartProjectInput.builder()
                .addData(getTestInputData())
                .addExtension(AppArtifactKey.fromString("io.quarkus:quarkus-resteasy"))
                .addExtension(AppArtifactKey.fromString("io.quarkus:quarkus-kotlin"))
                .putData(RESTEASY_EXAMPLE_PACKAGE_NAME.getKey(), "com.andy")
                .putData(RESTEASY_EXAMPLE_RESOURCE_CLASS_NAME.getKey(), "BonjourResource")
                .putData(RESTEASY_EXAMPLE_RESOURCE_PATH.getKey(), "/bonjour")
                .build();
        final Path projectDir = testDirPath.resolve("resteasy-kotlin-custom");
        getCatalog().createProject(input).generate(projectDir);

        checkMaven(projectDir);
        checkReadme(projectDir);
        checkDockerfiles(projectDir, BuildTool.MAVEN);
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
        final QuarkusCodestartProjectInput input = QuarkusCodestartProjectInput.builder()
                .addData(getTestInputData())
                .addExtension(AppArtifactKey.fromString("io.quarkus:quarkus-resteasy"))
                .addExtension(AppArtifactKey.fromString("io.quarkus:quarkus-scala"))
                .putData(RESTEASY_EXAMPLE_PACKAGE_NAME.getKey(), "com.andy")
                .putData(RESTEASY_EXAMPLE_RESOURCE_CLASS_NAME.getKey(), "BonjourResource")
                .putData(RESTEASY_EXAMPLE_RESOURCE_PATH.getKey(), "/bonjour")
                .build();
        final Path projectDir = testDirPath.resolve("resteasy-scala-custom");
        getCatalog().createProject(input).generate(projectDir);

        checkMaven(projectDir);
        checkReadme(projectDir);
        checkDockerfiles(projectDir, BuildTool.MAVEN);
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
    void generateCodestartProjectMavenDefaultJava() throws IOException {
        final QuarkusCodestartProjectInput input = QuarkusCodestartProjectInput.builder()
                .addData(getTestInputData())
                .build();
        final Path projectDir = testDirPath.resolve("maven-default-java");
        getCatalog().createProject(input).generate(projectDir);

        checkMaven(projectDir);
        checkReadme(projectDir);
        checkDockerfiles(projectDir, BuildTool.MAVEN);
        checkConfigProperties(projectDir);

        assertThat(projectDir.resolve("src/main/java/org/acme/resteasy/ExampleResource.java")).exists();
        assertThat(projectDir.resolve("src/test/java/org/acme/resteasy/ExampleResourceTest.java")).exists();
        assertThat(projectDir.resolve("src/test/java/org/acme/resteasy/NativeExampleResourceIT.java")).exists();
    }

    @Test
    void generateCodestartProjectMavenResteasyJava() throws IOException {
        final QuarkusCodestartProjectInput input = QuarkusCodestartProjectInput.builder()
                .addExtension(AppArtifactKey.fromString("io.quarkus:quarkus-resteasy"))
                .addData(getTestInputData())
                .build();
        final Path projectDir = testDirPath.resolve("maven-resteasy-java");
        getCatalog().createProject(input).generate(projectDir);

        checkMaven(projectDir);
        checkReadme(projectDir);
        checkDockerfiles(projectDir, BuildTool.MAVEN);
        checkConfigProperties(projectDir);

        assertThat(projectDir.resolve("src/main/java/org/acme/resteasy/ExampleResource.java")).exists();
        assertThat(projectDir.resolve("src/test/java/org/acme/resteasy/ExampleResourceTest.java")).exists();
        assertThat(projectDir.resolve("src/test/java/org/acme/resteasy/NativeExampleResourceIT.java")).exists();
    }

    @Test
    void generateCodestartProjectMavenConfigYamlJava() throws IOException {
        final QuarkusCodestartProjectInput input = QuarkusCodestartProjectInput.builder()
                .addExtension(AppArtifactKey.fromString("io.quarkus:quarkus-config-yaml"))
                .addData(getTestInputData())
                .build();
        final Path projectDir = testDirPath.resolve("maven-yaml-java");
        getCatalog().createProject(input).generate(projectDir);

        checkMaven(projectDir);
        checkReadme(projectDir);
        checkDockerfiles(projectDir, BuildTool.MAVEN);
        checkConfigYaml(projectDir);

        assertThat(projectDir.resolve("src/main/java/org/acme/config/GreetingResource.java")).exists();
    }

    @Test
    void generateCodestartProjectMavenResteasyKotlin() throws IOException {
        final QuarkusCodestartProjectInput input = QuarkusCodestartProjectInput.builder()
                .addExtension(AppArtifactKey.fromString("io.quarkus:quarkus-resteasy"))
                .addExtension(AppArtifactKey.fromString("io.quarkus:quarkus-kotlin"))
                .addData(getTestInputData())
                .build();
        final Path projectDir = testDirPath.resolve("maven-resteasy-kotlin");
        getCatalog().createProject(input).generate(projectDir);

        checkMaven(projectDir);
        checkReadme(projectDir);
        checkDockerfiles(projectDir, BuildTool.MAVEN);
        checkConfigProperties(projectDir);

        assertThat(projectDir.resolve("src/main/kotlin/org/acme/resteasy/ExampleResource.kt")).exists();
        assertThat(projectDir.resolve("src/test/kotlin/org/acme/resteasy/ExampleResourceTest.kt")).exists();
        assertThat(projectDir.resolve("src/test/kotlin/org/acme/resteasy/NativeExampleResourceIT.kt")).exists();
    }

    @Test
    void generateCodestartProjectMavenResteasyScala() throws IOException {
        final QuarkusCodestartProjectInput input = QuarkusCodestartProjectInput.builder()
                .addExtension(AppArtifactKey.fromString("io.quarkus:quarkus-resteasy"))
                .addExtension(AppArtifactKey.fromString("io.quarkus:quarkus-scala"))
                .addData(getTestInputData())
                .build();
        final Path projectDir = testDirPath.resolve("maven-resteasy-scala");
        getCatalog().createProject(input).generate(projectDir);

        checkMaven(projectDir);
        checkReadme(projectDir);
        checkDockerfiles(projectDir, BuildTool.MAVEN);
        checkConfigProperties(projectDir);

        assertThat(projectDir.resolve("src/main/scala/org/acme/resteasy/ExampleResource.scala")).exists();
        assertThat(projectDir.resolve("src/test/scala/org/acme/resteasy/ExampleResourceTest.scala")).exists();
        assertThat(projectDir.resolve("src/test/scala/org/acme/resteasy/NativeExampleResourceIT.scala")).exists();
    }

    @Test
    void generateCodestartProjectGradleResteasyJava() throws IOException {
        final QuarkusCodestartProjectInput input = QuarkusCodestartProjectInput.builder()
                .buildTool(BuildTool.GRADLE)
                .addCodestart("gradle")
                .addExtension(AppArtifactKey.fromString("io.quarkus:quarkus-resteasy"))
                .addData(getTestInputData())
                .build();
        final Path projectDir = testDirPath.resolve("gradle-resteasy-java");
        getCatalog().createProject(input).generate(projectDir);

        checkGradle(projectDir);
        checkReadme(projectDir);
        checkDockerfiles(projectDir, BuildTool.GRADLE);
        checkConfigProperties(projectDir);

        assertThat(projectDir.resolve("src/main/java/org/acme/resteasy/ExampleResource.java")).exists();
        assertThat(projectDir.resolve("src/test/java/org/acme/resteasy/ExampleResourceTest.java")).exists();
        assertThat(projectDir.resolve("src/native-test/java/org/acme/resteasy/NativeExampleResourceIT.java")).exists();
    }

    @Test
    void generateCodestartProjectGradleResteasyKotlin() throws IOException {
        final QuarkusCodestartProjectInput input = QuarkusCodestartProjectInput.builder()
                .buildTool(BuildTool.GRADLE)
                .addExtension(AppArtifactKey.fromString("io.quarkus:quarkus-resteasy"))
                .addExtension(AppArtifactKey.fromString("io.quarkus:quarkus-kotlin"))
                .addCodestart("gradle")
                .addData(getTestInputData())
                .build();
        final Path projectDir = testDirPath.resolve("gradle-resteasy-kotlin");
        getCatalog().createProject(input).generate(projectDir);

        checkGradle(projectDir);
        checkReadme(projectDir);
        checkDockerfiles(projectDir, BuildTool.GRADLE);
        checkConfigProperties(projectDir);

        assertThat(projectDir.resolve("src/main/kotlin/org/acme/resteasy/ExampleResource.kt")).exists();
        assertThat(projectDir.resolve("src/test/kotlin/org/acme/resteasy/ExampleResourceTest.kt")).exists();
        assertThat(projectDir.resolve("src/native-test/kotlin/org/acme/resteasy/NativeExampleResourceIT.kt")).exists();
    }

    @Test
    void generateCodestartProjectGradleResteasyScala() throws IOException {
        final QuarkusCodestartProjectInput input = QuarkusCodestartProjectInput.builder()
                .buildTool(BuildTool.GRADLE)
                .addExtension(AppArtifactKey.fromString("io.quarkus:quarkus-resteasy"))
                .addExtension(AppArtifactKey.fromString("io.quarkus:quarkus-scala"))
                .addData(getTestInputData())
                .build();
        final Path projectDir = testDirPath.resolve("gradle-resteasy-scala");
        getCatalog().createProject(input).generate(projectDir);

        checkGradle(projectDir);
        checkReadme(projectDir);
        checkDockerfiles(projectDir, BuildTool.GRADLE);
        checkConfigProperties(projectDir);

        assertThat(projectDir.resolve("src/main/scala/org/acme/resteasy/ExampleResource.scala")).exists();
        assertThat(projectDir.resolve("src/test/scala/org/acme/resteasy/ExampleResourceTest.scala")).exists();
        assertThat(projectDir.resolve("src/native-test/scala/org/acme/resteasy/NativeExampleResourceIT.scala")).exists();
    }

    @Test
    void generateCodestartProjectGradleWithKotlinDslResteasyJava() throws IOException {
        final QuarkusCodestartProjectInput input = QuarkusCodestartProjectInput.builder()
                .buildTool(BuildTool.GRADLE_KOTLIN_DSL)
                .addExtension(AppArtifactKey.fromString("io.quarkus:quarkus-resteasy"))
                .addData(getTestInputData())
                .build();
        final Path projectDir = testDirPath.resolve("gradle-kotlin-dsl-resteasy-java");
        getCatalog().createProject(input).generate(projectDir);

        checkGradleWithKotlinDsl(projectDir);
        checkReadme(projectDir);
        checkDockerfiles(projectDir, BuildTool.GRADLE_KOTLIN_DSL);
        checkConfigProperties(projectDir);

        assertThat(projectDir.resolve("src/main/java/org/acme/resteasy/ExampleResource.java")).exists();
    }

    @Test
    void generateCodestartProjectGradleWithKotlinDslResteasyKotlin() throws IOException {
        final QuarkusCodestartProjectInput input = QuarkusCodestartProjectInput.builder()
                .buildTool(BuildTool.GRADLE_KOTLIN_DSL)
                .addExtension(AppArtifactKey.fromString("io.quarkus:quarkus-resteasy"))
                .addExtension(AppArtifactKey.fromString("io.quarkus:quarkus-kotlin"))
                .addData(getTestInputData())
                .build();
        final Path projectDir = testDirPath.resolve("gradle-kotlin-dsl-resteasy-kotlin");
        getCatalog().createProject(input).generate(projectDir);

        checkGradleWithKotlinDsl(projectDir);
        checkReadme(projectDir);
        checkDockerfiles(projectDir, BuildTool.GRADLE_KOTLIN_DSL);
        checkConfigProperties(projectDir);

        assertThat(projectDir.resolve("src/main/kotlin/org/acme/resteasy/ExampleResource.kt")).exists();
    }

    @Test
    void generateCodestartProjectGradleWithKotlinDslResteasyScala() throws IOException {
        final QuarkusCodestartProjectInput input = QuarkusCodestartProjectInput.builder()
                .buildTool(BuildTool.GRADLE_KOTLIN_DSL)
                .addExtension(AppArtifactKey.fromString("io.quarkus:quarkus-resteasy"))
                .addExtension(AppArtifactKey.fromString("io.quarkus:quarkus-scala"))
                .addData(getTestInputData())
                .build();
        final Path projectDir = testDirPath.resolve("gradle-kotlin-dsl-resteasy-scala");
        getCatalog().createProject(input).generate(projectDir);

        checkGradleWithKotlinDsl(projectDir);
        checkReadme(projectDir);
        checkDockerfiles(projectDir, BuildTool.GRADLE_KOTLIN_DSL);
        checkConfigProperties(projectDir);

        assertThat(projectDir.resolve("src/main/scala/org/acme/resteasy/ExampleResource.scala")).exists();
    }

    @Test
    void generateCodestartProjectQute() throws IOException {
        final QuarkusCodestartProjectInput input = QuarkusCodestartProjectInput.builder()
                .addExtension(AppArtifactKey.fromString("io.quarkus:quarkus-qute"))
                .addCodestart("qute")
                .addData(getTestInputData())
                .build();
        final Path projectDir = testDirPath.resolve("maven-qute");
        getCatalog().createProject(input).generate(projectDir);

        checkMaven(projectDir);
        checkReadme(projectDir);
        checkDockerfiles(projectDir, BuildTool.MAVEN);
        checkConfigProperties(projectDir);

        assertThat(projectDir.resolve("src/main/java/org/acme/qute/Item.java")).exists();
    }

    @Test
    public void generateGradleWrapperGithubAction() throws Exception {
        final QuarkusCodestartProjectInput input = QuarkusCodestartProjectInput.builder()
                .buildTool(BuildTool.GRADLE)
                .addData(getTestInputData())
                .addCodestarts(Collections.singletonList("github-action"))
                .putData(QuarkusCodestartData.DataKey.JAVA_VERSION.getKey(), System.getProperty("java.specification.version"))
                .build();
        Path projectDir = testDirPath.resolve("gradle-github");
        getCatalog().createProject(input).generate(projectDir);

        checkGradle(projectDir);

        assertThat(projectDir.resolve(".github/workflows/ci.yml"))
                .exists()
                .satisfies(checkContains("run: ./gradlew build"));
    }

    @Test
    public void generateGradleNoWrapperGithubAction() throws Exception {
        final QuarkusCodestartProjectInput input = QuarkusCodestartProjectInput.builder()
                .buildTool(BuildTool.GRADLE)
                .noBuildToolWrapper()
                .addData(getTestInputData())
                .addCodestarts(Collections.singletonList("github-action"))
                .putData(QuarkusCodestartData.DataKey.JAVA_VERSION.getKey(), System.getProperty("java.specification.version"))
                .build();
        Path projectDir = testDirPath.resolve("gradle-nowrapper-github");
        getCatalog().createProject(input).generate(projectDir);

        checkGradle(projectDir);

        assertThat(projectDir.resolve(".github/workflows/ci.yml"))
                .exists()
                .satisfies(checkContains("uses: eskatos/gradle-command-action@v1"))
                .satisfies(checkContains("arguments: build"));
    }

    private void checkNoExample(Path projectDir) {
        assertThat(projectDir.resolve("src/main/java")).doesNotExist();
        assertThat(projectDir.resolve("src/main/kotlin")).doesNotExist();
        assertThat(projectDir.resolve("src/main/scala")).doesNotExist();
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
                .satisfies(checkContains("registry.access.redhat.com/ubi8/ubi-minimal:8.1"))
                .satisfies(checkContains("ARG JAVA_PACKAGE=java-11-openjdk-headless"))
                .satisfies(checkContains("ENTRYPOINT [ \"/deployments/run-java.sh\" ]"));
        assertThat(projectDir.resolve("src/main/docker/Dockerfile.fast-jar")).exists()
                .satisfies(checkContains("./mvnw package -Dquarkus.package.type=fast-jar"))
                .satisfies(checkContains("docker build -f src/main/docker/Dockerfile.fast-jar"))
                .satisfies(checkContains("registry.access.redhat.com/ubi8/ubi-minimal:8.1"))
                .satisfies(checkContains("ARG JAVA_PACKAGE=java-11-openjdk-headless"))
                .satisfies(checkContains("ENTRYPOINT [ \"/deployments/run-java.sh\" ]"));
        assertThat(projectDir.resolve("src/main/docker/Dockerfile.native")).exists()
                .satisfies(checkContains("./mvnw package -Pnative"))
                .satisfies(checkContains("registry.access.redhat.com/ubi8/ubi-minimal:8.1"))
                .satisfies(checkContains("CMD [\"./application\", \"-Dquarkus.http.host=0.0.0.0\"]"));
    }

    private void checkDockerfilesWithGradle(Path projectDir) {
        assertThat(projectDir.resolve(".dockerignore")).exists();
        assertThat(projectDir.resolve("src/main/docker/Dockerfile.jvm")).exists()
                .satisfies(checkContains("./gradlew build"))
                .satisfies(checkContains("docker build -f src/main/docker/Dockerfile.jvm"))
                .satisfies(checkContains("registry.access.redhat.com/ubi8/ubi-minimal:8.1"))
                .satisfies(checkContains("ARG JAVA_PACKAGE=java-11-openjdk-headless"))
                .satisfies(checkContains("ENTRYPOINT [ \"/deployments/run-java.sh\" ]"));
        assertThat(projectDir.resolve("src/main/docker/Dockerfile.fast-jar")).exists()
                .satisfies(checkContains("./gradlew build -Dquarkus.package.type=fast-jar"))
                .satisfies(checkContains("docker build -f src/main/docker/Dockerfile.fast-jar"))
                .satisfies(checkContains("registry.access.redhat.com/ubi8/ubi-minimal:8.1"))
                .satisfies(checkContains("ARG JAVA_PACKAGE=java-11-openjdk-headless"))
                .satisfies(checkContains("ENTRYPOINT [ \"/deployments/run-java.sh\" ]"));
        assertThat(projectDir.resolve("src/main/docker/Dockerfile.native")).exists()
                .satisfies(checkContains("./gradlew build -Dquarkus.package.type=native"))
                .satisfies(checkContains("registry.access.redhat.com/ubi8/ubi-minimal:8.1"))
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
        return QuarkusCodestartCatalog.fromQuarkusPlatformDescriptor(getPlatformDescriptor());
    }

}
