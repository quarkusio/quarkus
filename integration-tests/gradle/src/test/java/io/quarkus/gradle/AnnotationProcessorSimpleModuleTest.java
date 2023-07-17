package io.quarkus.gradle;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

public class AnnotationProcessorSimpleModuleTest extends QuarkusGradleWrapperTestBase {

    @Test
    public void shouldRunTestCorrectly() throws Exception {
        final File projectDir = getProjectDir("annotation-processor-simple-module");

        BuildResult buildResult = runGradleWrapper(projectDir, "clean", "test");

        assertThat(BuildResult.isSuccessful(buildResult.getTasks().get(":test"))).isTrue();
    }

    @Test
    public void shouldContainsPanacheMarkerFile() throws Exception {
        final File projectDir = getProjectDir("annotation-processor-simple-module");

        BuildResult buildResult = runGradleWrapper(projectDir, "clean", "quarkusBuild");

        assertThat(BuildResult.isSuccessful(buildResult.getTasks().get(":quarkusBuild"))).isTrue();
        File buildDir = new File(projectDir, "build");

        Path metaInfDir = buildDir.toPath().resolve("classes").resolve("java").resolve("main").resolve("META-INF");
        assertThat(metaInfDir.resolve("panache-archive.marker")).exists();
    }
}
