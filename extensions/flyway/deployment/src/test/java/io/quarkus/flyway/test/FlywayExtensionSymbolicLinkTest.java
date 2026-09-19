package io.quarkus.flyway.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import jakarta.inject.Inject;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

/**
 * Test that filesystem locations work correctly with symbolic links.
 */
public class FlywayExtensionSymbolicLinkTest {

    private static final Path tempDir = createTempDirectory();
    private static boolean symlinkSupported = true;

    private static Path createTempDirectory() {
        try {
            // Use prefix with space to test Windows path handling with spaces
            return Files.createTempDirectory("flyway symlink test ");
        } catch (IOException e) {
            throw new RuntimeException("Failed to create temp directory", e);
        }
    }

    @Inject
    Flyway flyway;

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("filesystem-symlink-test.properties", "application.properties"))
            .setBeforeAllCustomizer(() -> {
                try {
                    setupSymbolicLink();
                } catch (IOException | UnsupportedOperationException e) {
                    // On Windows, symbolic link creation requires administrator privileges
                    // or Developer Mode to be enabled. Skip the test if it fails.
                    symlinkSupported = false;
                }
            })
            .overrideConfigKey("quarkus.flyway.locations",
                    "filesystem:" + normalizePathForFlyway(tempDir.resolve("symlink-to-migrations")));

    @BeforeAll
    static void setupSymbolicLink() throws IOException {
        // Get the actual migration directory (use core migrations as the target)
        Path targetDir = Paths.get("src/test/resources/flyway-test/core/sql").toAbsolutePath();

        // Create symbolic link
        Path symlinkPath = tempDir.resolve("symlink-to-migrations");
        Files.createSymbolicLink(symlinkPath, targetDir);
    }

    @AfterAll
    static void cleanupTempDirectory() throws IOException {
        if (Files.exists(tempDir)) {
            Files.walk(tempDir)
                    .sorted((a, b) -> b.compareTo(a))
                    .forEach(p -> {
                        try {
                            Files.delete(p);
                        } catch (IOException e) {
                            // Ignore
                        }
                    });
        }
    }

    @Test
    @DisplayName("Symbolic links to migration directories should work")
    public void testSymbolicLinkSupport() {
        // Skip test if symbolic links are not supported (e.g., Windows without admin privileges)
        assumeTrue(symlinkSupported, "Symbolic links not supported on this system");

        // Should find the migration through the symbolic link
        assertThat(flyway.info().all()).hasSizeGreaterThanOrEqualTo(1);
        assertThat(flyway.info().current().getVersion().toString()).isEqualTo("1.0.0");
    }

    /**
     * Normalizes path for Flyway by converting to absolute path and using forward slashes.
     * This ensures consistent path handling across Windows and Unix platforms.
     */
    private static String normalizePathForFlyway(Path path) {
        return path.toAbsolutePath().toString().replace('\\', '/');
    }
}
