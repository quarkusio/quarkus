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
import io.quarkus.devtools.project.codegen.SourceType;
import io.quarkus.platform.tools.config.QuarkusPlatformConfig;
import io.quarkus.test.devmode.util.DevModeTestUtils;

public class QuarkusPluginFunctionalTest extends QuarkusGradleWrapperTestBase {

    private File projectRoot;

    @BeforeEach
    void setUp(@TempDir File projectRoot) {
        this.projectRoot = projectRoot;
    }

    @Test
    public void canRunListExtensions() throws Exception {
        createProject(SourceType.JAVA);

        BuildResult build = runGradleWrapper(projectRoot, "listExtensions");

        assertThat(build.getTasks().get(":listExtensions")).isEqualTo(BuildResult.SUCCESS_OUTCOME);
        assertThat(build.getOutput()).contains("Quarkus - Core");
    }

    @Test
    public void canGenerateConfig() throws Exception {
        createProject(SourceType.JAVA);

        BuildResult build = runGradleWrapper(projectRoot, "generateConfig");

        assertThat(build.getTasks().get(":generateConfig")).isEqualTo(BuildResult.SUCCESS_OUTCOME);
        assertThat(projectRoot.toPath().resolve("src/main/resources/application.properties.example")).exists();
    }

    @ParameterizedTest(name = "Build {0} project")
    @EnumSource(SourceType.class)
    public void canBuild(SourceType sourceType) throws Exception {
        createProject(sourceType);

        BuildResult build = runGradleWrapper(projectRoot, "build", "--stacktrace");

        assertThat(build.getTasks().get(":build")).isEqualTo(BuildResult.SUCCESS_OUTCOME);
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
        assertThat(firstBuild.getTasks().get(":quarkusBuild")).isEqualTo(BuildResult.SUCCESS_OUTCOME);

        BuildResult secondBuild = runGradleWrapper(projectRoot, "quarkusBuild", "--stacktrace");
        assertThat(secondBuild.getTasks().get(":quarkusBuild")).isEqualTo(BuildResult.UPTODATE_OUTCOME);
    }

    @Test
    public void canDetectResourceChangeWhenBuilding() throws Exception {
        createProject(SourceType.JAVA);

        BuildResult firstBuild = runGradleWrapper(projectRoot, "quarkusBuild", "--stacktrace");
        assertThat(firstBuild.getTasks().get(":quarkusBuild")).isEqualTo(BuildResult.SUCCESS_OUTCOME);

        final File applicationProperties = projectRoot.toPath().resolve("src/main/resources/application.properties").toFile();
        DevModeTestUtils.filter(applicationProperties, ImmutableMap.of("# Configuration file", "quarkus.http.port=8888"));

        BuildResult secondBuild = runGradleWrapper(projectRoot, "quarkusBuild", "--stacktrace");
        assertThat(secondBuild.getTasks().get(":quarkusBuild")).isEqualTo(BuildResult.SUCCESS_OUTCOME);
    }

    @Test
    public void canDetectClassChangeWhenBuilding() throws Exception {
        createProject(SourceType.JAVA);

        BuildResult firstBuild = runGradleWrapper(projectRoot, "quarkusBuild", "--stacktrace");
        assertThat(firstBuild.getTasks().get(":quarkusBuild")).isEqualTo(BuildResult.SUCCESS_OUTCOME);

        final File greetingResourceFile = projectRoot.toPath().resolve("src/main/java/org/acme/GreetingResource.java").toFile();
        DevModeTestUtils.filter(greetingResourceFile, ImmutableMap.of("\"/greeting\"", "\"/test/hello\""));

        BuildResult secondBuild = runGradleWrapper(projectRoot, "quarkusBuild", "--stacktrace");
        assertThat(secondBuild.getTasks().get(":quarkusBuild")).isEqualTo(BuildResult.SUCCESS_OUTCOME);
    }

    @Test
    public void canDetectClasspathChangeWhenBuilding() throws Exception {
        createProject(SourceType.JAVA);

        BuildResult firstBuild = runGradleWrapper(projectRoot, "quarkusBuild", "--stacktrace");
        assertThat(firstBuild.getTasks().get(":quarkusBuild")).isEqualTo(BuildResult.SUCCESS_OUTCOME);

        runGradleWrapper(projectRoot, "addExtension", "--extensions=hibernate-orm");
        BuildResult secondBuild = runGradleWrapper(projectRoot, "quarkusBuild", "--stacktrace");
        assertThat(secondBuild.getTasks().get(":quarkusBuild")).isEqualTo(BuildResult.SUCCESS_OUTCOME);
    }

    @Test
    public void canDetectOutputChangeWhenBuilding() throws Exception {
        createProject(SourceType.JAVA);

        BuildResult firstBuild = runGradleWrapper(projectRoot, "quarkusBuild", "--stacktrace");

        assertThat(firstBuild.getTasks().get(":quarkusBuild")).isEqualTo(BuildResult.SUCCESS_OUTCOME);
        Path runnerJar = projectRoot.toPath().resolve("build").resolve("foo-1.0.0-SNAPSHOT-runner.jar");
        Files.delete(runnerJar);

        BuildResult secondBuild = runGradleWrapper(projectRoot, "quarkusBuild", "--stacktrace");

        assertThat(secondBuild.getTasks().get(":quarkusBuild")).isEqualTo(BuildResult.SUCCESS_OUTCOME);
        assertThat(runnerJar).exists();
    }

    @Test
    public void canDetectSystemPropertyChangeWhenBuilding() throws Exception {
        createProject(SourceType.JAVA);

        BuildResult firstBuild = runGradleWrapper(projectRoot, "quarkusBuild", "--stacktrace");

        assertThat(firstBuild.getTasks().get(":quarkusBuild")).isEqualTo(BuildResult.SUCCESS_OUTCOME);
        assertThat(projectRoot.toPath().resolve("build").resolve("foo-1.0.0-SNAPSHOT-runner.jar")).exists();

        BuildResult secondBuild = runGradleWrapper(projectRoot, "quarkusBuild", "-Dquarkus.package.type=fast-jar");

        assertThat(secondBuild.getTasks().get(":quarkusBuild")).isEqualTo(BuildResult.SUCCESS_OUTCOME);
        assertThat(projectRoot.toPath().resolve("build").resolve("quarkus-app")).exists();
    }

    @Test
    public void canRunTest() throws Exception {
        createProject(SourceType.JAVA);

        BuildResult buildResult = runGradleWrapper(projectRoot, "test", "--stacktrace");

        assertThat(buildResult.getTasks().get(":test")).isEqualTo(BuildResult.SUCCESS_OUTCOME);
    }

    private void createProject(SourceType sourceType) throws Exception {
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
