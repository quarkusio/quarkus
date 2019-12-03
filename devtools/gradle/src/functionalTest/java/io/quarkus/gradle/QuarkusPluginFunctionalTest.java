package io.quarkus.gradle;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import io.quarkus.cli.commands.CreateProject;
import io.quarkus.cli.commands.writer.FileProjectWriter;
import io.quarkus.generators.BuildTool;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

public class QuarkusPluginFunctionalTest {

    @Test
    public void canRunListExtensions(@TempDir File projectRoot) throws IOException {
        createProject(projectRoot);

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
    public void canBuild(@TempDir File projectRoot) throws IOException {
        createProject(projectRoot);

        BuildResult build = GradleRunner.create()
                .forwardOutput()
                .withPluginClasspath()
                .withArguments(arguments("quarkusBuild"))
                .withProjectDir(projectRoot)
                .build();

        assertThat(build.task(":quarkusBuild").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
    }

    private List<String> arguments(String argument) {
        List<String> arguments = new ArrayList<>();
        arguments.add(argument);
        String mavenRepoLocal = System.getProperty("maven.repo.local", System.getenv("MAVEN_LOCAL_REPO"));
        if (mavenRepoLocal != null) {
            arguments.add("-Dmaven.repo.local=" + mavenRepoLocal);
        }
        return arguments;
    }

    private void createProject(@TempDir File projectRoot) throws IOException {
        assertThat(new CreateProject(new FileProjectWriter(projectRoot))
                           .groupId("com.acme.foo")
                           .artifactId("foo")
                           .version("1.0.0-SNAPSHOT")
                           .buildTool(BuildTool.GRADLE)
                           .doCreateProject(new HashMap<>()))
                .withFailMessage("Project was not created")
                .isTrue();
    }
}