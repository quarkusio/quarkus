package io.quarkus.gradle;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

public class JavaPlatformWithEagerResolutionTest extends QuarkusGradleWrapperTestBase {

    @Test
    public void shouldImportConditionalDependency() throws IOException, URISyntaxException, InterruptedException {

        final File projectDir = getProjectDir("java-platform-with-eager-resolution-project");

        runGradleWrapper(projectDir, "clean", ":quarkusBuild");

        final File buildDir = new File(projectDir, "build");

        final Path quarkusOutput = buildDir.toPath().resolve("quarkus-app");
        assertThat(quarkusOutput.resolve("quarkus-run.jar")).exists();
    }
}
