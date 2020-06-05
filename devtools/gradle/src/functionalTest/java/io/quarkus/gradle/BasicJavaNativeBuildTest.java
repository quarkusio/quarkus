package io.quarkus.gradle;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

public class BasicJavaNativeBuildTest extends QuarkusGradleTestBase  {

    @Test
    public void shouldIgnoreNativeArgs() throws Exception{
        final File projectDir = getProjectDir("basic-java-native-module");

        BuildResult build = GradleRunner.create()
                .forwardOutput()
                .withPluginClasspath()
                .withArguments(arguments("clean", "quarkusBuild"))
                .withProjectDir(projectDir)
                .build();

        assertThat(build.task(":quarkusBuild").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
        assertThat(projectDir.toPath().resolve("build").resolve("foo-1.0.0-SNAPSHOT-runner.jar")).exists();
        assertThat(projectDir.toPath().resolve("build").resolve("foo-1.0.0-SNAPSHOT-runner")).doesNotExist();
    }
}
