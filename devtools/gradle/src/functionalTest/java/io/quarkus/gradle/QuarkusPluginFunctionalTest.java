package io.quarkus.gradle;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.quarkus.platform.descriptor.QuarkusPlatformDescriptor;
import io.quarkus.platform.tools.config.QuarkusPlatformConfig;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import io.quarkus.cli.commands.CreateProject;
import io.quarkus.cli.commands.writer.FileProjectWriter;
import io.quarkus.generators.BuildTool;
import io.quarkus.generators.SourceType;

public class QuarkusPluginFunctionalTest extends QuarkusGradleTestBase {

    private File projectRoot;

    @BeforeEach
    void setUp(@TempDir File projectRoot) {
        this.projectRoot = projectRoot;
    }

    @Test
    public void canRunListExtensions() throws IOException {
        createProject(SourceType.JAVA);

        BuildResult build = GradleRunner.create()
                .forwardOutput()
                .withPluginClasspath()
                .withArguments(arguments("listExtensions"))
                .withProjectDir(projectRoot)
                .build();

        assertThat(build.task(":listExtensions").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
        assertThat(build.getOutput()).contains("Quarkus - Core");
    }

    @Test
    public void canGenerateConfig() throws IOException {
        createProject(SourceType.JAVA);

        BuildResult build = GradleRunner.create()
                .forwardOutput()
                .withPluginClasspath()
                .withArguments(arguments("generateConfig"))
                .withProjectDir(projectRoot)
                .build();

        assertThat(build.task(":generateConfig").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
        assertThat(projectRoot.toPath().resolve("src/main/resources/application.properties.example")).exists();
    }


    @ParameterizedTest(name = "Build {0} project")
    @EnumSource(SourceType.class)
    public void canBuild(SourceType sourceType) throws IOException, InterruptedException {
        createProject(sourceType);

        BuildResult build = GradleRunner.create()
                .forwardOutput()
                .withPluginClasspath()
                .withArguments(arguments("build", "--stacktrace"))
                .withProjectDir(projectRoot)
                .build();

        assertThat(build.task(":build").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
        // gradle build should not build the native image
        assertThat(build.task(":buildNative")).isNull();
    }

    private void createProject(SourceType sourceType) throws IOException {
        Map<String, Object> context = new HashMap<>();
        context.put("path", "/greeting");
        assertThat(new CreateProject(new FileProjectWriter(projectRoot),
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