package io.quarkus.devtools.codestarts.quarkus;

import static io.quarkus.devtools.codestarts.quarkus.QuarkusCodestartData.QuarkusDataKey.PROJECT_PACKAGE_NAME;
import static io.quarkus.devtools.codestarts.quarkus.QuarkusCodestartData.QuarkusDataKey.RESTEASY_CODESTART_RESOURCE_CLASS_NAME;
import static io.quarkus.devtools.codestarts.quarkus.QuarkusCodestartData.QuarkusDataKey.RESTEASY_CODESTART_RESOURCE_PATH;
import static io.quarkus.devtools.testing.FakeExtensionCatalog.FAKE_QUARKUS_CODESTART_CATALOG;
import static io.quarkus.devtools.testing.SnapshotTesting.assertThatMatchSnapshot;
import static io.quarkus.devtools.testing.SnapshotTesting.checkContains;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import io.quarkus.devtools.project.BuildTool;
import io.quarkus.devtools.testing.FakeExtensionCatalog;
import io.quarkus.devtools.testing.SnapshotTesting;
import io.quarkus.devtools.testing.codestarts.QuarkusCodestartTesting;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.ArtifactKey;

class QuarkusCodestartGenerationTest {

    private static final Path testDirPath = Paths.get("target/quarkus-codestart-gen-test");

    @BeforeAll
    static void setUp() throws Throwable {
        SnapshotTesting.deleteTestDirectory(testDirPath.toFile());
    }

    private Map<String, Object> getGenerationTestInputData() {
        return QuarkusCodestartTesting.getMockedTestInputData(Collections.emptyMap());
    }

    @Test
    void generateDefault(TestInfo testInfo) throws Throwable {
        final QuarkusCodestartProjectInput input = newInputBuilder()
                .noCode()
                .noDockerfiles()
                .noBuildToolWrapper()
                .addData(getGenerationTestInputData())
                .addBoms(QuarkusCodestartTesting.getPlatformBoms())
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

    private static QuarkusCodestartProjectInputBuilder newInputBuilder() {
        return QuarkusCodestartProjectInput.builder().defaultCodestart(FakeExtensionCatalog.getDefaultCodestart());
    }

    @Test
    void generateRESTEasyJavaCustom(TestInfo testInfo) throws Throwable {
        final QuarkusCodestartProjectInput input = newInputBuilder()
                .addData(getGenerationTestInputData())
                .addExtension(ArtifactKey.fromString("io.quarkus:quarkus-resteasy"))
                .putData(PROJECT_PACKAGE_NAME.key(), "com.andy")
                .putData(RESTEASY_CODESTART_RESOURCE_CLASS_NAME.key(), "BonjourResource")
                .putData(RESTEASY_CODESTART_RESOURCE_PATH.key(), "/bonjour")
                .build();
        final Path projectDir = testDirPath.resolve("resteasy-java-custom");
        getCatalog().createProject(input).generate(projectDir);

        checkMaven(projectDir);
        checkReadme(projectDir);
        checkDockerfiles(projectDir, BuildTool.MAVEN);
        checkConfigProperties(projectDir);

        assertThatMatchSnapshot(testInfo, projectDir, "src/main/java/com/andy/BonjourResource.java");
        assertThatMatchSnapshot(testInfo, projectDir, "src/test/java/com/andy/BonjourResourceIT.java");
    }

    @Test
    void generateMavenWithCustomDep(TestInfo testInfo) throws Throwable {
        final QuarkusCodestartProjectInput input = newInputBuilder()
                .addData(getGenerationTestInputData())
                .addBoms(QuarkusCodestartTesting.getPlatformBoms())
                .addExtension(ArtifactCoords.fromString("io.quarkus:quarkus-resteasy:1.8"))
                .addExtension(ArtifactCoords.fromString("commons-io:commons-io:2.5"))

                .build();
        final Path projectDir = testDirPath.resolve("maven-custom-dep");
        getCatalog().createProject(input).generate(projectDir);

        checkMaven(projectDir);
        assertThatMatchSnapshot(testInfo, projectDir, "pom.xml")
                .satisfies(checkContains("<dependency>\n" +
                        "            <groupId>io.quarkus</groupId>\n" +
                        "            <artifactId>quarkus-resteasy</artifactId>\n" +
                        "            <version>1.8</version>\n" +
                        "        </dependency>"))
                .satisfies(checkContains("<dependency>\n" +
                        "            <groupId>io.quarkus</groupId>\n" +
                        "            <artifactId>quarkus-resteasy</artifactId>\n" +
                        "            <version>1.8</version>\n" +
                        "        </dependency>"));
    }

    @Test
    void generateRESTEasyKotlinCustom(TestInfo testInfo) throws Throwable {
        final QuarkusCodestartProjectInput input = newInputBuilder()
                .addData(getGenerationTestInputData())
                .addExtension(ArtifactKey.fromString("io.quarkus:quarkus-resteasy"))
                .addExtension(ArtifactKey.fromString("io.quarkus:quarkus-kotlin"))
                .putData(PROJECT_PACKAGE_NAME.key(), "com.andy")
                .putData(RESTEASY_CODESTART_RESOURCE_CLASS_NAME.key(), "BonjourResource")
                .putData(RESTEASY_CODESTART_RESOURCE_PATH.key(), "/bonjour")
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

        assertThatMatchSnapshot(testInfo, projectDir, "src/test/kotlin/com/andy/BonjourResourceIT.kt")
                .satisfies(checkContains("package com.andy"))
                .satisfies(checkContains("class BonjourResourceIT : BonjourResourceTest"));
    }

    @Test
    void generateRESTEasyScalaCustom(TestInfo testInfo) throws Throwable {
        final QuarkusCodestartProjectInput input = newInputBuilder()
                .addData(getGenerationTestInputData())
                .addExtension(ArtifactKey.fromString("io.quarkus:quarkus-resteasy"))
                .addExtension(ArtifactKey.fromString("io.quarkus:quarkus-scala"))
                .putData(PROJECT_PACKAGE_NAME.key(), "com.andy")
                .putData(RESTEASY_CODESTART_RESOURCE_CLASS_NAME.key(), "BonjourResource")
                .putData(RESTEASY_CODESTART_RESOURCE_PATH.key(), "/bonjour")
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

        assertThatMatchSnapshot(testInfo, projectDir, "src/test/scala/com/andy/BonjourResourceIT.scala")
                .satisfies(checkContains("package com.andy"))
                .satisfies(checkContains("class BonjourResourceIT extends BonjourResourceTest"));
    }

    @Test
    void generateMavenDefaultJava(TestInfo testInfo) throws Throwable {
        final QuarkusCodestartProjectInput input = newInputBuilder()
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
        assertThatMatchSnapshot(testInfo, projectDir, "src/test/java/org/acme/GreetingResourceIT.java");
    }

    @Test
    void generateGradleDefaultJava(TestInfo testInfo) throws Throwable {
        final QuarkusCodestartProjectInput input = newInputBuilder()
                .buildTool(BuildTool.GRADLE)
                .addData(getGenerationTestInputData())
                .build();
        final Path projectDir = testDirPath.resolve("gradle-default-java");
        getCatalog().createProject(input).generate(projectDir);

        checkGradle(projectDir);
        checkReadme(projectDir);
        checkDockerfiles(projectDir, BuildTool.GRADLE);
        checkConfigProperties(projectDir);

        assertThatMatchSnapshot(testInfo, projectDir, "src/main/java/org/acme/GreetingResource.java");
        assertThatMatchSnapshot(testInfo, projectDir, "src/test/java/org/acme/GreetingResourceTest.java");
        assertThatMatchSnapshot(testInfo, projectDir, "src/native-test/java/org/acme/GreetingResourceIT.java");
    }

    @Test
    void generateMavenResteasyJava(TestInfo testInfo) throws Throwable {
        final QuarkusCodestartProjectInput input = newInputBuilder()
                .addExtension(ArtifactKey.fromString("io.quarkus:quarkus-resteasy"))
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
        assertThatMatchSnapshot(testInfo, projectDir, "src/test/java/org/acme/GreetingResourceIT.java");
    }

    @Test
    void generateMavenConfigYamlJava(TestInfo testInfo) throws Throwable {
        final QuarkusCodestartProjectInput input = newInputBuilder()
                .addExtension(ArtifactKey.fromString("io.quarkus:quarkus-config-yaml"))
                .addData(getGenerationTestInputData())
                .build();
        final Path projectDir = testDirPath.resolve("maven-yaml-java");
        getCatalog().createProject(input).generate(projectDir);

        checkMaven(projectDir);
        checkReadme(projectDir);
        checkDockerfiles(projectDir, BuildTool.MAVEN);
        checkConfigYaml(projectDir);
    }

    @Test
    public void generateGradleWrapperGithubAction(TestInfo testInfo) throws Throwable {
        final QuarkusCodestartProjectInput input = newInputBuilder()
                .buildTool(BuildTool.GRADLE)
                .addData(getGenerationTestInputData())
                .addCodestarts(Collections.singletonList("tooling-github-action"))
                .build();
        Path projectDir = testDirPath.resolve("gradle-github");
        getCatalog().createProject(input).generate(projectDir);

        checkGradle(projectDir);

        assertThatMatchSnapshot(testInfo, projectDir, ".github/workflows/ci.yml")
                .satisfies(
                        checkContains("cache: gradle"),
                        checkContains("run: ./gradlew build"));
    }

    @Test
    public void generateMavenGithubAction(TestInfo testInfo) throws Throwable {
        final QuarkusCodestartProjectInput input = newInputBuilder()
                .buildTool(BuildTool.MAVEN)
                .addData(getGenerationTestInputData())
                .addCodestarts(Collections.singletonList("tooling-github-action"))
                .build();
        Path projectDir = testDirPath.resolve("maven-github");
        getCatalog().createProject(input).generate(projectDir);

        assertThatMatchSnapshot(testInfo, projectDir, ".github/workflows/ci.yml")
                .satisfies(checkContains("cache: maven"));
    }

    @Test
    public void generateGradleNoWrapperGithubAction(TestInfo testInfo) throws Throwable {
        final QuarkusCodestartProjectInput input = newInputBuilder()
                .buildTool(BuildTool.GRADLE)
                .noBuildToolWrapper()
                .addData(getGenerationTestInputData())
                .addCodestarts(Collections.singletonList("tooling-github-action"))
                .build();
        Path projectDir = testDirPath.resolve("gradle-nowrapper-github");
        getCatalog().createProject(input).generate(projectDir);

        checkGradle(projectDir);

        assertThatMatchSnapshot(testInfo, projectDir, ".github/workflows/ci.yml")
                .satisfies(
                        checkContains("uses: gradle/actions/setup-gradle"),
                        checkContains("cache: gradle"));
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
                .satisfies(checkContains("registry.access.redhat.com/ubi8/openjdk-17:"))
                .satisfies(checkContains("ENV JAVA_APP_JAR=\"/deployments/quarkus-run.jar\""))
                .satisfies(checkContains("ENTRYPOINT [ \"/opt/jboss/container/java/run/run-java.sh\" ]"));
        assertThat(projectDir.resolve("src/main/docker/Dockerfile.legacy-jar")).exists()
                .satisfies(checkContains("./mvnw package -Dquarkus.package.jar.type=legacy-jar"))
                .satisfies(checkContains("docker build -f src/main/docker/Dockerfile.legacy-jar"))
                .satisfies(checkContains("registry.access.redhat.com/ubi8/openjdk-17:"))
                .satisfies(checkContains("EXPOSE 8080"))
                .satisfies(checkContains("USER 185"))
                .satisfies(checkContains("ENV JAVA_APP_JAR=\"/deployments/quarkus-run.jar\""))
                .satisfies(checkContains("ENTRYPOINT [ \"/opt/jboss/container/java/run/run-java.sh\" ]"));
        assertThat(projectDir.resolve("src/main/docker/Dockerfile.native-micro")).exists()
                .satisfies(checkContains("./mvnw package -Dnative"))
                .satisfies(checkContains("quay.io/quarkus/quarkus-micro-image"))
                .satisfies(checkContains("ENTRYPOINT [\"./application\", \"-Dquarkus.http.host=0.0.0.0\"]"));
        assertThat(projectDir.resolve("src/main/docker/Dockerfile.native")).exists()
                .satisfies(checkContains("./mvnw package -Dnative"))
                .satisfies(checkContains("registry.access.redhat.com/ubi8/ubi-minimal"))
                .satisfies(checkContains("ENTRYPOINT [\"./application\", \"-Dquarkus.http.host=0.0.0.0\"]"));
    }

    private void checkDockerfilesWithGradle(Path projectDir) {
        assertThat(projectDir.resolve(".dockerignore")).exists();
        assertThat(projectDir.resolve("src/main/docker/Dockerfile.jvm")).exists()
                .satisfies(checkContains("./gradlew build"))
                .satisfies(checkContains("docker build -f src/main/docker/Dockerfile.jvm"))
                .satisfies(checkContains("registry.access.redhat.com/ubi8/openjdk-17:"))
                .satisfies(checkContains("ENV JAVA_APP_JAR=\"/deployments/quarkus-run.jar\""))
                .satisfies(checkContains("ENTRYPOINT [ \"/opt/jboss/container/java/run/run-java.sh\" ]"));
        assertThat(projectDir.resolve("src/main/docker/Dockerfile.legacy-jar")).exists()
                .satisfies(checkContains("./gradlew build -Dquarkus.package.jar.type=legacy-jar"))
                .satisfies(checkContains("docker build -f src/main/docker/Dockerfile.legacy-jar"))
                .satisfies(checkContains("registry.access.redhat.com/ubi8/openjdk-17:"))
                .satisfies(checkContains("EXPOSE 8080"))
                .satisfies(checkContains("USER 185"))
                .satisfies(checkContains("ENV JAVA_APP_JAR=\"/deployments/quarkus-run.jar\""))
                .satisfies(checkContains("ENTRYPOINT [ \"/opt/jboss/container/java/run/run-java.sh\" ]"));
        assertThat(projectDir.resolve("src/main/docker/Dockerfile.native-micro")).exists()
                .satisfies(checkContains("./gradlew build -Dquarkus.native.enabled=true"))
                .satisfies(checkContains("quay.io/quarkus/quarkus-micro-image:2.0"))
                .satisfies(checkContains("ENTRYPOINT [\"./application\", \"-Dquarkus.http.host=0.0.0.0\"]"));
        assertThat(projectDir.resolve("src/main/docker/Dockerfile.native")).exists()
                .satisfies(checkContains("./gradlew build -Dquarkus.native.enabled=true"))
                .satisfies(checkContains("registry.access.redhat.com/ubi8/ubi-minimal"))
                .satisfies(checkContains("ENTRYPOINT [\"./application\", \"-Dquarkus.http.host=0.0.0.0\"]"));
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
                .satisfies(checkContains("group = 'org.test'"))
                .satisfies(checkContains("version = '1.0.0-codestart'"));
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

    private QuarkusCodestartCatalog getCatalog() {
        return FAKE_QUARKUS_CODESTART_CATALOG;
    }

}
