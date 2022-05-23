package io.quarkus.gradle;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.google.common.collect.ImmutableMap;

import io.quarkus.devtools.commands.CreateProject;
import io.quarkus.devtools.project.BuildTool;
import io.quarkus.devtools.project.QuarkusProjectHelper;
import io.quarkus.devtools.project.codegen.SourceType;
import io.quarkus.test.devmode.util.DevModeTestUtils;

public class QuarkusPluginFunctionalTest extends QuarkusGradleDevToolsTestBase {

    private File projectRoot;

    @BeforeEach
    void setUp(@TempDir File projectRoot) {
        this.projectRoot = projectRoot;
    }

    @ParameterizedTest(name = "Build {0} project")
    @EnumSource(SourceType.class)
    public void canBuild(SourceType sourceType) throws Exception {
        createProject(sourceType);

        BuildResult build = runGradleWrapper(projectRoot, "build", "--stacktrace");

        assertThat(BuildResult.isSuccessful(build.getTasks().get(":build"))).isTrue();
        // gradle build should not build the native image
        assertThat(build.getTasks().get(":buildNative")).isNull();
        Path buildDir = projectRoot.toPath().resolve("build");
        assertThat(buildDir).exists();
        assertThat(buildDir.resolve("foo-1.0.0-SNAPSHOT-runner")).doesNotExist();
    }

    @Test
    public void canDetectUpToDateBuild() throws Exception {
        createProject(SourceType.JAVA);

        BuildResult firstBuild = runGradleWrapper(projectRoot, "quarkusBuild", "--stacktrace");
        assertThat(BuildResult.isSuccessful(firstBuild.getTasks().get(":quarkusBuild"))).isTrue();

        BuildResult secondBuild = runGradleWrapper(projectRoot, "quarkusBuild", "--stacktrace");
        assertThat(secondBuild.getTasks().get(":quarkusBuild")).isEqualTo(BuildResult.UPTODATE_OUTCOME);
    }

    @Test
    public void canDetectResourceChangeWhenBuilding() throws Exception {
        createProject(SourceType.JAVA);

        BuildResult firstBuild = runGradleWrapper(projectRoot, "quarkusBuild", "--stacktrace");
        assertThat(BuildResult.isSuccessful(firstBuild.getTasks().get(":quarkusBuild"))).isTrue();

        final File applicationProperties = projectRoot.toPath().resolve("src/main/resources/application.properties").toFile();
        Files.write(applicationProperties.toPath(), "quarkus.http.port=8888".getBytes());

        BuildResult secondBuild = runGradleWrapper(projectRoot, "quarkusBuild", "--stacktrace");
        assertThat(BuildResult.isSuccessful(secondBuild.getTasks().get(":quarkusBuild"))).isTrue();
    }

    @Test
    public void canDetectClassChangeWhenBuilding() throws Exception {
        createProject(SourceType.JAVA);

        BuildResult firstBuild = runGradleWrapper(projectRoot, "quarkusBuild", "--stacktrace");
        assertThat(BuildResult.isSuccessful(firstBuild.getTasks().get(":quarkusBuild"))).isTrue();

        final File greetingResourceFile = projectRoot.toPath().resolve("src/main/java/org/acme/foo/GreetingResource.java")
                .toFile();
        DevModeTestUtils.filter(greetingResourceFile, ImmutableMap.of("\"/greeting\"", "\"/test/hello\""));

        BuildResult secondBuild = runGradleWrapper(projectRoot, "quarkusBuild", "--stacktrace");
        assertThat(BuildResult.isSuccessful(secondBuild.getTasks().get(":quarkusBuild"))).isTrue();
    }

    @Test
    public void canDetectClasspathChangeWhenBuilding() throws Exception {
        createProject(SourceType.JAVA);

        BuildResult firstBuild = runGradleWrapper(projectRoot, "quarkusBuild", "--stacktrace");
        assertThat(BuildResult.isSuccessful(firstBuild.getTasks().get(":quarkusBuild"))).isTrue();

        runGradleWrapper(projectRoot, "addExtension", "--extensions=hibernate-orm");
        BuildResult secondBuild = runGradleWrapper(projectRoot, "quarkusBuild", "--stacktrace");
        assertThat(BuildResult.isSuccessful(secondBuild.getTasks().get(":quarkusBuild"))).isTrue();
    }

    @Test
    public void canDetectOutputChangeWhenBuilding() throws Exception {
        createProject(SourceType.JAVA);

        BuildResult firstBuild = runGradleWrapper(projectRoot, "quarkusBuild", "--stacktrace");

        assertThat(BuildResult.isSuccessful(firstBuild.getTasks().get(":quarkusBuild"))).isTrue();
        Path runnerJar = projectRoot.toPath().resolve("build").resolve("quarkus-app").resolve("quarkus-run.jar");
        Files.delete(runnerJar);

        BuildResult secondBuild = runGradleWrapper(projectRoot, "quarkusBuild", "--stacktrace");

        assertThat(BuildResult.isSuccessful(secondBuild.getTasks().get(":quarkusBuild"))).isTrue();
        assertThat(runnerJar).exists();
    }

    @Test
    public void canDetectUpToDateTests() throws Exception {
        createProject(SourceType.JAVA);

        BuildResult firstBuild = runGradleWrapper(projectRoot, "test");

        assertThat(BuildResult.isSuccessful(firstBuild.getTasks().get(":test"))).isTrue();

        BuildResult secondBuild = runGradleWrapper(projectRoot, "test");

        assertThat(secondBuild.getTasks().get(":test")).isEqualTo(BuildResult.UPTODATE_OUTCOME);
    }

    @Test
    public void canDetectSystemPropertyChangeWhenBuilding() throws Exception {
        createProject(SourceType.JAVA);

        BuildResult firstBuild = runGradleWrapper(projectRoot, "quarkusBuild", "--stacktrace");

        assertThat(BuildResult.isSuccessful(firstBuild.getTasks().get(":quarkusBuild"))).isTrue();
        assertThat(projectRoot.toPath().resolve("build").resolve("quarkus-app").resolve("quarkus-run.jar")).exists();

        BuildResult secondBuild = runGradleWrapper(projectRoot, "quarkusBuild", "-Dquarkus.package.type=fast-jar");

        assertThat(BuildResult.isSuccessful(secondBuild.getTasks().get(":quarkusBuild"))).isTrue();
        assertThat(projectRoot.toPath().resolve("build").resolve("quarkus-app")).exists();
    }

    @Test
    public void canRunTest() throws Exception {
        createProject(SourceType.JAVA);

        BuildResult buildResult = runGradleWrapper(projectRoot, "test", "--stacktrace");

        assertThat(BuildResult.isSuccessful(buildResult.getTasks().get(":test"))).isTrue();
    }

    @Test
    public void generateCodeBeforeTests() throws Exception {
        createProject(SourceType.JAVA);

        BuildResult firstBuild = runGradleWrapper(projectRoot, "test", "--stacktrace");
        assertThat(firstBuild.getOutput()).contains("Task :quarkusGenerateCode");
        assertThat(firstBuild.getOutput()).contains("Task :quarkusGenerateCodeTests");
        assertThat(BuildResult.isSuccessful(firstBuild.getTasks().get(":test"))).isTrue();
    }

    private void createProject(SourceType sourceType) throws Exception {
        Map<String, Object> context = new HashMap<>();
        context.put("path", "/greeting");
        assertThat(new CreateProject(QuarkusProjectHelper.getProject(projectRoot.toPath(),
                BuildTool.GRADLE))
                        .groupId("com.acme.foo")
                        .artifactId("foo")
                        .version("1.0.0-SNAPSHOT")
                        .packageName("org.acme.foo")
                        .sourceType(sourceType)
                        .doCreateProject(context))
                                .withFailMessage("Project was not created")
                                .isTrue();
    }
}
