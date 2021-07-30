package io.quarkus.config;

import static io.quarkus.extest.deployment.MapBuildTimeConfigBuildStep.TEST_MAP_CONFIG_MARKER;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.extest.deployment.MapBuildTimeConfigBuildStep;
import io.quarkus.test.QuarkusUnitTest;

public class BuildTimeConfigTest {

    static final File testFile;

    static {
        try {
            testFile = File.createTempFile(TEST_MAP_CONFIG_MARKER, "tmp");
            testFile.deleteOnExit();
        } catch (IOException e) {
            throw new RuntimeException("Failed to create temporary file for " + BuildTimeConfigTest.class);
        }
    }

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .overrideConfigKey("test-map-config", testFile.getAbsolutePath());

    /**
     * The checks are done before the test is started, in {@link io.quarkus.extest.deployment.MapBuildTimeConfigBuildStep}
     * here we only verify if the build step did its job
     */
    @Test
    void shouldWork() throws IOException {
        String markerFilePath = ConfigProvider.getConfig().getValue(TEST_MAP_CONFIG_MARKER, String.class);
        String content = Files.readString(Paths.get(markerFilePath));
        assertThat(content).isEqualTo(MapBuildTimeConfigBuildStep.INVOKED);
    }
}
