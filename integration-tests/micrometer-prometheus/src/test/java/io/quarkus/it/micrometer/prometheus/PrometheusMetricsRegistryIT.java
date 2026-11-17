package io.quarkus.it.micrometer.prometheus;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusIntegrationTest;

@QuarkusIntegrationTest
class PrometheusMetricsRegistryIT extends PrometheusMetricsRegistryTest {

    @Test
    void nativeBuildHasNoFieldValueTransformationWarnings() throws IOException {
        Path executable = Path.of(System.getProperty("native.image.path"));
        if (!Files.exists(executable)) {
            executable = executable.resolveSibling(executable.getFileName() + ".exe");
        }
        assertThat(executable).isRegularFile();

        Path stdout = executable.resolveSibling(executable.getFileName() + "-build-stdout.log");
        Path stderr = executable.resolveSibling(executable.getFileName() + "-build-stderr.log");
        assertThat(stdout).isRegularFile();
        assertThat(stderr).isRegularFile();
        String buildOutput = Files.readString(stdout);
        assertThat(buildOutput).contains("GraalVM Native Image", "Finished generating");
        assertThat(buildOutput + Files.readString(stderr))
                .doesNotContain("automatic field value transformation failed");
    }
}
