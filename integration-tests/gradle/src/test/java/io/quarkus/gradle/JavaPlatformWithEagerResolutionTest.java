package io.quarkus.gradle;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
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

    @Test
    public void dryRunShouldNotResolveDeploymentConfigurations()
            throws IOException, InterruptedException {

        var projectDir = getProjectDir("java-platform-with-eager-resolution-project");
        var initScript = projectDir.toPath().resolve("log-resolution.init.gradle.kts");
        Files.writeString(initScript, """
                allprojects {
                	configurations.configureEach {
                		incoming.beforeResolve {
                			println("QUARKUS_TEST_RESOLVED:${project.path}:${name}")
                		}
                	}
                }
                """);

        var result = runGradleWrapper(projectDir, "test", "--dry-run", "--no-configuration-cache",
                "-I", initScript.toString());

        assertThat(result.getOutput())
                .contains(":quarkusGenerateAppModel SKIPPED")
                .contains(":quarkusGenerateCode SKIPPED")
                .contains(":quarkusGenerateTestAppModel SKIPPED")
                .contains(":quarkusGenerateCodeTests SKIPPED")
                .doesNotContain("QUARKUS_TEST_RESOLVED:::quarkusProdRuntimeClasspathConfigurationDeployment")
                .doesNotContain("QUARKUS_TEST_RESOLVED:::quarkusTestRuntimeClasspathConfigurationDeployment");
    }
}
