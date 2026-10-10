package io.quarkus.gradle.init;

import static org.assertj.core.api.Assertions.assertThat;
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;

import io.quarkus.devtools.testing.RegistryClientTest;

/**
 * Runs the actual {@code gradle init -Dorg.gradle.buildinit.specs=...} command line against this plugin published
 * to a local Maven repository, the way a real consumer resolves it, instead of calling
 * {@link QuarkusAppInitGenerator} directly like {@link QuarkusAppInitGeneratorTest} does. This is what exercises
 * Gradle's own spec resolution and its post-generation wrapper step.
 */
class QuarkusInitSpecsPluginFunctionalTest {

    @RegisterExtension
    static final RegistryClientTest registryClientTest = new RegistryClientTest();

    @TempDir
    Path workDir;

    @Test
    void resolvesThePluginAndLetsGradleGenerateTheWrapper() throws IOException {
        // The init script must live outside the project directory: gradle init refuses to run
        // against a non-empty directory.
        Path initScript = workDir.resolve("init.gradle.kts");
        Path projectDir = Files.createDirectory(workDir.resolve("project"));
        Files.writeString(initScript, """
                beforeSettings {
                    pluginManagement {
                        repositories {
                            maven { url = uri("%s") }
                            mavenLocal()
                            mavenCentral()
                        }
                    }
                }
                """.formatted(System.getProperty("functionalTest.repo")));

        BuildResult result = GradleRunner.create()
                .withProjectDir(projectDir.toFile())
                .withEnvironment(environmentOutsideOfMaven())
                .withArguments(List.of("--init-script", initScript.toString(), "init",
                        "-Dorg.gradle.buildinit.specs=io.quarkus.init:" + System.getProperty("project.version"),
                        "-Dquarkus.tools.config=" + System.getProperty("quarkus.tools.config"),
                        "-DquarkusRegistryClient=true", "--type", "quarkus-app"))
                .build();

        assertThat(result.task(":init").getOutcome()).isEqualTo(SUCCESS);
        assertThat(projectDir.resolve("build.gradle.kts")).exists();
        assertThat(projectDir.resolve("gradlew")).exists();
    }

    /**
     * The Maven build wrapping this module exports {@code MAVEN_CMD_LINE_ARGS}, which the nested daemon would
     * inherit. When it is set, {@code BootstrapMavenOptions} parses the Maven command line through the thread
     * context class loader, which inside a Gradle daemon cannot see this plugin's classes. A real
     * {@code gradle init} doesn't run inside a Maven build, so don't pass it along.
     */
    private static Map<String, String> environmentOutsideOfMaven() {
        Map<String, String> environment = new HashMap<>(System.getenv());
        environment.remove("MAVEN_CMD_LINE_ARGS");
        return environment;
    }
}
