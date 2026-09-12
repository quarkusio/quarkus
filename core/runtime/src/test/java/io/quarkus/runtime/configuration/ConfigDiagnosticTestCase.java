package io.quarkus.runtime.configuration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

import org.junit.jupiter.api.Test;

public class ConfigDiagnosticTestCase {

    /**
     * The scan for unknown configuration files is only there to warn, so a directory that cannot be
     * read (for example {@code C:\Windows\system32\config} when that is the working directory) must
     * be skipped rather than fail the startup.
     */
    @Test
    public void unreadableDirectoryIsSkipped() throws IOException {
        assumeTrue(FileSystems.getDefault().supportedFileAttributeViews().contains("posix"),
                "POSIX permissions are required");
        assumeFalse("root".equals(System.getProperty("user.name")), "root ignores permissions");

        Path directory = Files.createTempDirectory("config-diagnostic-");
        Path applicationProperties = directory.resolve("application.properties");
        Files.writeString(applicationProperties, "");
        Set<PosixFilePermission> originalPermissions = Files.getPosixFilePermissions(directory);
        Files.setPosixFilePermissions(directory, Set.of());
        try {
            Set<Path> configFiles = assertDoesNotThrow(() -> ConfigDiagnostic.configFiles(directory));
            assertTrue(configFiles.isEmpty());
        } finally {
            Files.setPosixFilePermissions(directory, originalPermissions);
            Files.delete(applicationProperties);
            Files.delete(directory);
        }
    }
}
