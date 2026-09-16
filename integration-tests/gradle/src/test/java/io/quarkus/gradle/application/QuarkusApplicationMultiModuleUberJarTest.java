package io.quarkus.gradle.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.quarkus.gradle.BuildResult;

class QuarkusApplicationMultiModuleUberJarTest extends QuarkusApplicationGradleTestBase {

    @Test
    void uberJarIncludesLibraryProject() throws Exception {
        File projectDir = getProjectDir("application-plugin/multi-module-uber");

        BuildResult result = runApplicationGradleWrapper(projectDir, "clean", ":application:quarkusUberBuild");

        assertThat(result.unsuccessfulTasks()).isEmpty();
        assertThat(result.getTasks()).containsKey(":application:quarkusUberBuild");

        Path applicationBuildDirectory = projectDir.toPath().resolve(Path.of("application", "build"));
        Path runner = applicationBuildDirectory.resolve(
                Path.of("quarkus-builds", "uber", "package", "application-1.0.0-SNAPSHOT-runner.jar"));
        assertJarApplication(runner, applicationBuildDirectory.resolve("uber-output.log"), Map.of(),
                "hello from the common project");
    }
}
