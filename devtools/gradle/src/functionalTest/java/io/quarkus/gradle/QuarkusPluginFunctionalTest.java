package io.quarkus.gradle;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.google.common.collect.ImmutableMap;

import io.quarkus.cli.commands.CreateProject;
import io.quarkus.cli.commands.project.BuildTool;
import io.quarkus.generators.SourceType;
import io.quarkus.platform.tools.config.QuarkusPlatformConfig;
import io.quarkus.test.devmode.util.DevModeTestUtils;

public class QuarkusPluginFunctionalTest extends QuarkusGradleTestBase {

    private File projectRoot;

    @BeforeEach
    void setUp(@TempDir File projectRoot) {
        this.projectRoot = projectRoot;
    }

    @Test
    public void canRunListExtensions() throws IOException {
        createProject(SourceType.JAVA);

        BuildResult build = runTask(arguments("listExtensions"));

        assertThat(build.task(":listExtensions").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
        assertThat(build.getOutput()).contains("Quarkus - Core");
    }

    @Test
    public void canGenerateConfig() throws IOException {
        createProject(SourceType.JAVA);

        BuildResult build = runTask(arguments("generateConfig"));

        assertThat(build.task(":generateConfig").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
        assertThat(projectRoot.toPath().resolve("src/main/resources/application.properties.example")).exists();
    }

    @ParameterizedTest(name = "Build {0} project")
    @EnumSource(SourceType.class)
    public void canBuild(SourceType sourceType) throws IOException, InterruptedException {
        createProject(sourceType);

        BuildResult build = runTask(arguments("build", "--stacktrace"));

        assertThat(build.task(":build").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
        // gradle build should not build the native image
        assertThat(build.task(":buildNative")).isNull();
        Path buildDir = projectRoot.toPath().resolve("build");
        assertThat(buildDir).exists();
        assertThat(buildDir.resolve("foo-1.0.0-SNAPSHOT-runner")).doesNotExist();
    }

    @Test
    public void canDetectUpToDateBuild() throws IOException {
        createProject(SourceType.JAVA);

        BuildResult firstBuild = runTask(arguments("quarkusBuild", "--stacktrace"));
        assertThat(firstBuild.task(":quarkusBuild").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);

        BuildResult secondBuild = runTask(arguments("quarkusBuild", "--stacktrace"));
        assertThat(secondBuild.task(":quarkusBuild").getOutcome()).isEqualTo(TaskOutcome.UP_TO_DATE);
    }

    @Test
    public void canDetectResourceChangeWhenBuilding() throws IOException {
        createProject(SourceType.JAVA);

        BuildResult firstBuild = runTask(arguments("quarkusBuild", "--stacktrace"));
        assertThat(firstBuild.task(":quarkusBuild").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);

        final File applicationProperties = projectRoot.toPath().resolve("src/main/resources/application.properties").toFile();
        DevModeTestUtils.filter(applicationProperties, ImmutableMap.of("# Configuration file", "quarkus.http.port=8888"));

        BuildResult secondBuild = runTask(arguments("quarkusBuild", "--stacktrace"));
        assertThat(secondBuild.task(":quarkusBuild").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
    }

    @Test
    public void canDetectClassChangeWhenBuilding() throws IOException {
        createProject(SourceType.JAVA);

        BuildResult firstBuild = runTask(arguments("quarkusBuild", "--stacktrace"));
        assertThat(firstBuild.task(":quarkusBuild").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);

        final File greetingResourceFile = projectRoot.toPath().resolve("src/main/java/org/acme/GreetingResource.java").toFile();
        DevModeTestUtils.filter(greetingResourceFile, ImmutableMap.of("\"/greeting\"", "\"/test/hello\""));

        BuildResult secondBuild = runTask(arguments("quarkusBuild", "--stacktrace"));
        assertThat(secondBuild.task(":quarkusBuild").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
    }

    @Test
    public void canDetectClasspathChangeWhenBuilding() throws IOException {
        createProject(SourceType.JAVA);

        BuildResult firstBuild = runTask(arguments("quarkusBuild", "--stacktrace"));
        assertThat(firstBuild.task(":quarkusBuild").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);

        runTask(arguments("addExtension", "--extensions=hibernate-orm"));
        BuildResult secondBuild = runTask(arguments("quarkusBuild", "--stacktrace"));
        assertThat(secondBuild.task(":quarkusBuild").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
    }

    @Test
    public void canDetectOutputChangeWhenBuilding() throws IOException {
        createProject(SourceType.JAVA);

        BuildResult firstBuild = runTask(arguments("quarkusBuild", "--stacktrace"));

        assertThat(firstBuild.task(":quarkusBuild").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
        Path runnerJar = projectRoot.toPath().resolve("build").resolve("foo-1.0.0-SNAPSHOT-runner.jar");
        Files.delete(runnerJar);

        BuildResult secondBuild = runTask(arguments("quarkusBuild", "--stacktrace"));

        assertThat(secondBuild.task(":quarkusBuild").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
        assertThat(runnerJar).exists();
    }

    private BuildResult runTask(List<String> arguments) {
        return GradleRunner.create()
                .forwardOutput()
                .withPluginClasspath()
                .withArguments(arguments)
                .withProjectDir(projectRoot)
                .build();
    }

    private void createProject(SourceType sourceType) throws IOException {
        Map<String, Object> context = new HashMap<>();
        context.put("path", "/greeting");
        assertThat(new CreateProject(projectRoot.toPath(),
                                     QuarkusPlatformConfig.getGlobalDefault().getPlatformDescriptor())
                .groupId("com.acme.foo")
                .artifactId("foo")
                .version("1.0.0-SNAPSHOT")
                .buildTool(BuildTool.GRADLE)
                .className("org.acme.GreetingResource")
                .sourceType(sourceType)
                .doCreateProject(context))
                        .withFailMessage("Project was not created")
                        .isTrue();
    }
}