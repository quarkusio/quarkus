package io.quarkus.gradle;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * Integration test that verifies that file timestamps are preserved correctly
 * when copying files from build/quarkus-build/gen/quarkus-app to build/quarkus-app
 * during the quarkusBuild task execution.
 */
public class TimestampsComparisonTest extends QuarkusGradleWrapperTestBase {

    @Test
    public void testTimestampsPreservedInQuarkusApp() throws Exception {
        final File projectDir = getProjectDir("timestamps-comparison-test");

        runGradleWrapper(projectDir, "clean", "quarkusBuild");

        final Path quarkusApp = projectDir.toPath().resolve("build").resolve("quarkus-app");
        final Path buildGenQuarkusApp = projectDir.toPath().resolve("build").resolve("quarkus-build")
                .resolve("gen").resolve("quarkus-app");
        final Path buildAppQuarkusApp = projectDir.toPath().resolve("build").resolve("quarkus-build")
                .resolve("app").resolve("quarkus-app");
        final Path buildDep = projectDir.toPath().resolve("build").resolve("quarkus-build")
                .resolve("dep");

        assertThat(quarkusApp).exists();
        assertThat(buildGenQuarkusApp).exists();
        assertThat(buildAppQuarkusApp).exists();
        assertThat(buildDep).exists();

        // Collect timestamps from all directories
        Map<String, Long> quarkusAppTimestamps = collectTimestamps(quarkusApp);
        Map<String, Long> buildGenTimestamps = collectTimestamps(buildGenQuarkusApp);
        Map<String, Long> buildAppTimestamps = collectTimestamps(buildAppQuarkusApp);
        Map<String, Long> buildDepTimestamps = collectTimestamps(buildDep);

        // Compare timestamps for files that exist in build/quarkus-app
        // (complete set of files) and other locations.
        compareTimestamps(quarkusAppTimestamps, buildGenTimestamps);
        compareTimestamps(quarkusAppTimestamps, buildAppTimestamps);
        compareTimestamps(quarkusAppTimestamps, buildDepTimestamps);
    }

    /**
     * Compares timestamps between two maps and asserts equality for matching files.
     *
     * @param quarkusAppTimestamps the timestamps from quarkus-app
     *
     * @param otherLocationsTimestamps the timestamps from another location to compare against
     */
    private static void compareTimestamps(Map<String, Long> quarkusAppTimestamps, Map<String, Long> otherLocationsTimestamps) {
        for (Map.Entry<String, Long> entry : quarkusAppTimestamps.entrySet()) {
            String relativePath = entry.getKey();
            Long timestamp = entry.getValue();

            if (otherLocationsTimestamps.containsKey(relativePath)) {
                Long generatedTimestamp = otherLocationsTimestamps.get(relativePath);
                assertThat(timestamp)
                        .withFailMessage(
                                "Timestamp mismatch for file: %s\n" +
                                        "  quarkus-app timestamp: %d\n" +
                                        "  generated timestamp: %d",
                                relativePath, timestamp, generatedTimestamp)
                        .isEqualTo(generatedTimestamp);
            }
        }
    }

    /**
     * Recursively collects last modified timestamps for all files in a directory.
     *
     * @param baseDir the base directory to scan
     * @return a map of relative paths to their last modified timestamps
     */
    private Map<String, Long> collectTimestamps(Path baseDir) throws IOException {
        Map<String, Long> timestamps = new HashMap<>();

        try (Stream<Path> paths = Files.walk(baseDir)) {
            paths.filter(Files::isRegularFile)
                    .forEach(path -> {
                        String relativePath = baseDir.relativize(path).toString();
                        try {
                            long lastModified = Files.getLastModifiedTime(path).toMillis();
                            timestamps.put(relativePath, lastModified);
                        } catch (IOException e) {
                            throw new RuntimeException("Failed to get timestamp for: " + path, e);
                        }
                    });
        }

        return timestamps;
    }
}
