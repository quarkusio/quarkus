package io.quarkus.gradle;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class TaskDependenciesFunctionalTest {
    @TempDir
    Path projectDir;

    /**
     * Ensure that all Test tasks depend on the task
     * QuarkusTestConfig
     * This should include Test tasks created after the
     * plugin is applied (such as acceptTest in this test).
     */
    @Test
    public void shouldMakeTestTasksDependOnQuarkusTestConfig() throws IOException {
        createBuildFile();

        BuildResult build = GradleRunner.create()
                .withProjectDir(projectDir.toFile())
                .withArguments("-m", "acceptTest")
                .withPluginClasspath()
                .build();

        assertThat(build.getOutput()).contains(":quarkusTestConfig SKIPPED");
    }

    private void createBuildFile() throws IOException {
        Path buildFile = projectDir.resolve("build.gradle");

        try (InputStream is = getClass().getResourceAsStream("/TaskDependenciesFunctionalTest.gradle");
                BufferedInputStream bis = new BufferedInputStream(is)) {
            Files.copy(bis, buildFile);
        }
    }
}
