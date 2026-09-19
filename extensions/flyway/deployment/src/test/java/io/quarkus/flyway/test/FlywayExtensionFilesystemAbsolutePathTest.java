package io.quarkus.flyway.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import jakarta.inject.Inject;

import org.flywaydb.core.Flyway;
import org.h2.jdbc.JdbcSQLSyntaxErrorException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.agroal.api.AgroalDataSource;
import io.quarkus.agroal.DataSource;
import io.quarkus.flyway.FlywayDataSource;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * Test with absolute filesystem paths to reproduce bug #56777.
 * Without the fix, both datasources will have migrations from both directories applied.
 */
public class FlywayExtensionFilesystemAbsolutePathTest {

    private static final Path tempDir = createTempDirectory();

    private static Path createTempDirectory() {
        try {
            // Use prefix with space to test Windows path handling with spaces (e.g., "Program Files")
            return Files.createTempDirectory("flyway abs test ");
        } catch (IOException e) {
            throw new RuntimeException("Failed to create temp directory", e);
        }
    }

    @Inject
    Flyway flyway;

    @Inject
    @FlywayDataSource("reporting")
    Flyway flywayReporting;

    @Inject
    AgroalDataSource defaultDataSource;

    @Inject
    @DataSource("reporting")
    AgroalDataSource reportingDataSource;

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("filesystem-absolute-paths.properties", "application.properties"))
            .setBeforeAllCustomizer(() -> {
                try {
                    setupMigrationFiles();
                } catch (IOException e) {
                    throw new RuntimeException("Failed to setup migration files", e);
                }
            })
            .overrideConfigKey("quarkus.flyway.locations",
                    "filesystem:" + normalizePathForFlyway(tempDir.resolve("core/sql")))
            .overrideConfigKey("quarkus.flyway.reporting.locations",
                    "filesystem:" + normalizePathForFlyway(tempDir.resolve("reporting/sql")));

    @BeforeAll
    static void setupMigrationFiles() throws IOException {
        Path coreDir = tempDir.resolve("core/sql");
        Path reportingDir = tempDir.resolve("reporting/sql");
        Files.createDirectories(coreDir);
        Files.createDirectories(reportingDir);

        // Copy core migration file
        Path sourceBase = Paths.get("src/test/resources/flyway-test");
        Files.copy(
                sourceBase.resolve("core/sql/V1.0.0__Core_Create_Table.sql"),
                coreDir.resolve("V1.0.0__Core_Create_Table.sql"),
                StandardCopyOption.REPLACE_EXISTING);

        // Copy reporting migration file
        Files.copy(
                sourceBase.resolve("reporting/sql/V1.0.0__Reporting_Create_Table.sql"),
                reportingDir.resolve("V1.0.0__Reporting_Create_Table.sql"),
                StandardCopyOption.REPLACE_EXISTING);
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

    /**
     * Normalizes path for Flyway by converting to absolute path and using forward slashes.
     * This ensures consistent path handling across Windows and Unix platforms.
     */
    private static String normalizePathForFlyway(Path path) {
        return path.toAbsolutePath().toString().replace('\\', '/');
    }

    @Test
    @DisplayName("Default datasource should only have core migrations (not reporting)")
    public void testDefaultDataSourceOnlyHasCoreMigrations() throws SQLException {
        try (Connection connection = defaultDataSource.getConnection();
                Statement stat = connection.createStatement()) {
            // Verify core_operations table exists
            try (ResultSet rs = stat.executeQuery("SELECT COUNT(*) FROM core_operations")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).isEqualTo(2); // Should have 2 rows from V1.0.0
            }

            // Verify reporting_data table does NOT exist - this is the KEY test for the bug
            assertThrows(JdbcSQLSyntaxErrorException.class, () -> {
                try (ResultSet rs = stat.executeQuery("SELECT * FROM reporting_data")) {
                    // Should not reach here
                }
            }, "BUG: reporting_data table should NOT exist in default datasource - migrations are bleeding across datasources!");
        }

        // Should have 1 migration from core only
        assertThat(flyway.info().all()).hasSize(1);
    }

    @Test
    @DisplayName("Reporting datasource should only have reporting migrations (not core)")
    public void testReportingDataSourceOnlyHasReportingMigrations() throws SQLException {
        try (Connection connection = reportingDataSource.getConnection();
                Statement stat = connection.createStatement()) {
            // Verify reporting_data table exists
            try (ResultSet rs = stat.executeQuery("SELECT COUNT(*) FROM reporting_data")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).isEqualTo(0); // Empty table
            }

            // Verify core_operations table does NOT exist - this is the KEY test for the bug
            assertThrows(JdbcSQLSyntaxErrorException.class, () -> {
                try (ResultSet rs = stat.executeQuery("SELECT * FROM core_operations")) {
                    // Should not reach here
                }
            }, "BUG: core_operations table should NOT exist in reporting datasource - migrations are bleeding across datasources!");
        }

        // Should have 1 migration from reporting only
        assertThat(flywayReporting.info().all()).hasSize(1);
    }
}
