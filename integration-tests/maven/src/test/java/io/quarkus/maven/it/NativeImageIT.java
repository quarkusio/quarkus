package io.quarkus.maven.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import io.quarkus.maven.it.verifier.MavenProcessInvocationResult;
import io.quarkus.maven.it.verifier.RunningInvoker;

@EnableForNative
public class NativeImageIT extends MojoTestBase {

    /**
     * Tests that the {@code java.library.path} can be overridden/configurable by passing the system property
     * when launching the generated application's native image.
     *
     * @throws Exception
     */
    @Test
    public void testJavaLibraryPathAtRuntime() throws Exception {
        final File testDir = initProject("projects/native-image-app", "projects/native-image-app-output");
        final RunningInvoker running = new RunningInvoker(testDir, false);

        // trigger mvn package -Pnative -Dquarkus.ssl.native=true
        final String[] mvnArgs = new String[] { "package", "-DskipTests", "-Pnative", "-Dquarkus.ssl.native=true" };
        final MavenProcessInvocationResult result = running.execute(Arrays.asList(mvnArgs), Collections.emptyMap());
        await().atMost(5, TimeUnit.MINUTES).until(() -> result.getProcess() != null && !result.getProcess().isAlive());
        final String processLog = running.log();
        try {
            assertThat(processLog).containsIgnoringCase("BUILD SUCCESS");
        } catch (AssertionError ae) {
            // skip this test (instead of failing), if the native-image command wasn't available.
            // Bit brittle to rely on the log message, but it's OK in the context of this test
            Assumptions.assumeFalse(processLog.contains("Cannot find the `native-image"),
                    "Skipping test since native-image tool isn't available");
            // native-image command was available but the build failed for some reason, throw the original error
            throw ae;
        } finally {
            running.stop();
        }

        // now that the native image is built, run it
        final Path nativeImageRunner = testDir.toPath().toAbsolutePath().resolve(Paths.get("target/acme-1.0-SNAPSHOT-runner"));
        final Path tmpDir = Files.createTempDirectory("native-image-test");
        tmpDir.toFile().deleteOnExit();
        final Process nativeImageRunWithAdditionalLibPath = runNativeImage(nativeImageRunner,
                new String[] { "-Djava.library.path=" + tmpDir.toString() });
        try {
            final String response = getHttpResponse("/hello/javaLibraryPath");
            Assertions.assertTrue(response.contains(tmpDir.toString()),
                    "Response " + response + " for java.library.path was expected to contain the " + tmpDir + ", but didn't");
        } finally {
            nativeImageRunWithAdditionalLibPath.destroy();
        }

    }

    private static Process runNativeImage(final Path nativeImageRunnerFile, final String[] params) throws Exception {
        final List<String> commands = new ArrayList<>();
        commands.add(nativeImageRunnerFile.toString());
        if (params != null) {
            commands.addAll(Arrays.asList(params));
        }
        final ProcessBuilder processBuilder = new ProcessBuilder(commands.toArray(new String[0]));
        processBuilder.inheritIO();
        return processBuilder.start();
    }
}
