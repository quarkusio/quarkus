package io.quarkus.maven.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.maven.shared.invoker.MavenInvocationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.maven.it.verifier.MavenProcessInvocationResult;
import io.quarkus.maven.it.verifier.RunningInvoker;

@EnableForNative
public class NativeImageBundleIT extends MojoTestBase {

    private static final String NATIVE_IMAGE_BUNDLE_NAME = "foo.nib";

    /**
     * Tests that the GraalVM Native Image Bundle can be generated
     */
    @Test
    public void testNativeImageBundleCreation() throws MavenInvocationException, IOException {
        final File testDir = initProject("projects/native-image-bundle-build", "projects/native-image-bundle-build-output");
        final RunningInvoker running = new RunningInvoker(testDir, false);
        final List<String> mvnArgs = TestUtils.nativeArguments("package", "-Dnative");

        try {
            final MavenProcessInvocationResult result = running.execute(mvnArgs, Collections.emptyMap());
            await().atMost(3, TimeUnit.MINUTES).until(() -> result.getProcess() != null && !result.getProcess().isAlive());

            // Check the build was successful with dry run and actual build executed
            final String processLog = running.log();
            assertThat(processLog).containsSubsequence(
                    "Bundle written to",
                    NATIVE_IMAGE_BUNDLE_NAME,
                    "Native image Bundle available at",
                    "Quarkus augmentation completed",
                    "Quarkus augmentation completed",
                    "BUILD SUCCESS");

            // Verify that the bundle file exists
            Path nativeBundlePath = Path.of(testDir.getAbsolutePath(), "target", NATIVE_IMAGE_BUNDLE_NAME);
            Assertions.assertTrue(Files.exists(nativeBundlePath));
        } finally {
            running.stop();
        }
    }

}
