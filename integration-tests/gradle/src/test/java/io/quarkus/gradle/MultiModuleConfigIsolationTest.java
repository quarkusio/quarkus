package io.quarkus.gradle;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * Regression test for https://github.com/quarkusio/quarkus/issues/54095.
 *
 * <p>
 * In a multi-module Gradle build, each module's {@code quarkusAppPartsBuild} must see only its
 * own configuration. The test catches two leak symptoms per Gradle invocation:
 *
 * <ul>
 * <li><b>Crash.</b> Datasource configuration from the previous module leaks via the pooled
 * worker JVM and {@code AgroalProcessor} fails to load a driver missing from the classpath.
 * {@code runGradleWrapper} surfaces this as a build failure.</li>
 * <li><b>Silent value leak.</b> Module A sets {@code quarkus.package.output-name} through
 * its {@code quarkus { ... }} extension block. Module B leaves the property unset and falls
 * back to its own project base name. Without the worker JVM reset, A's system property
 * persists on the reused worker; {@code SysPropConfigSource} (ordinal 400) outranks the
 * "Build system" {@code PropertiesConfigSource} (ordinal 100); B then emits
 * {@code module-a-only-output.jar}. The file-name assertions below catch this.</li>
 * </ul>
 */
public class MultiModuleConfigIsolationTest extends QuarkusGradleWrapperTestBase {

    private static final String MODULE_A_OUTPUT_NAME = "module-a-only-output";
    private static final String MODULE_B_DEFAULT_OUTPUT_NAME = "module-b-1.0.0-SNAPSHOT";

    @Test
    public void testForwardOrderBuild() throws Exception {
        File projectDir = getProjectDir("multi-module-config-isolation");
        // `clean` stops one test method from inheriting build/ outputs from another. Without it,
        // Gradle marks both quarkusAppPartsBuild tasks up-to-date and skips them, sidestepping
        // the worker JVM reuse path the test exercises. Explicit task ordering pins module-a first.
        runGradleWrapper(projectDir, "clean", ":module-a:quarkusAppPartsBuild", ":module-b:quarkusAppPartsBuild");
        assertUserAppJarName(projectDir, "module-a", MODULE_A_OUTPUT_NAME);
        assertUserAppJarName(projectDir, "module-b", MODULE_B_DEFAULT_OUTPUT_NAME);
    }

    @Test
    public void testReverseOrderBuild() throws Exception {
        File projectDir = getProjectDir("multi-module-config-isolation");
        runGradleWrapper(projectDir, "clean", ":module-b:quarkusAppPartsBuild", ":module-a:quarkusAppPartsBuild");
        assertUserAppJarName(projectDir, "module-a", MODULE_A_OUTPUT_NAME);
        assertUserAppJarName(projectDir, "module-b", MODULE_B_DEFAULT_OUTPUT_NAME);
    }

    private static void assertUserAppJarName(File projectDir, String module, String expectedBaseName) throws Exception {
        // `quarkus.package.output-name` sets the `OutputTargetBuildItem` base name, which names
        // the user-app jar in the fast-jar layout at `build/quarkus-app/app/<base-name>.jar`.
        // Asserting on that path puts any leaked value directly into the failure message.
        Path appDir = projectDir.toPath().resolve(module).resolve("build/quarkus-app/app");
        assertThat(appDir).isDirectory();
        try (Stream<Path> entries = Files.list(appDir)) {
            assertThat(entries.map(p -> p.getFileName().toString()))
                    .as("user-app jar in %s should be named after %s, never after another module's override", appDir,
                            expectedBaseName)
                    .containsExactly(expectedBaseName + ".jar");
        }
    }
}
