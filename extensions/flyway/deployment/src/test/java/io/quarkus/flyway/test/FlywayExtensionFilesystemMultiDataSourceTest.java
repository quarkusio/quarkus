package io.quarkus.flyway.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import jakarta.inject.Inject;

import org.flywaydb.core.Flyway;
import org.h2.jdbc.JdbcSQLSyntaxErrorException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.agroal.api.AgroalDataSource;
import io.quarkus.agroal.DataSource;
import io.quarkus.flyway.FlywayDataSource;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * Test that verifies filesystem locations are properly filtered when using multiple datasources.
 * This test reproduces the bug reported in issue #56777 where migrations from sibling directories
 * were incorrectly being applied to all datasources regardless of their configured locations.
 */
public class FlywayExtensionFilesystemMultiDataSourceTest {

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
                    .addAsResource("filesystem-multi-datasources.properties", "application.properties"));

    @Test
    @DisplayName("Default datasource should only have core migrations applied")
    public void testDefaultDataSourceOnlyHasCoreMigrations() throws SQLException {
        // Verify core_operations table exists (from core migrations)
        try (Connection connection = defaultDataSource.getConnection();
                Statement stat = connection.createStatement()) {
            try (ResultSet rs = stat.executeQuery("SELECT COUNT(*) FROM core_operations")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).isEqualTo(2); // Should have 2 rows from V1.0.0
            }

            // Verify reporting_data table does NOT exist (should not be applied)
            assertThrows(JdbcSQLSyntaxErrorException.class, () -> {
                try (ResultSet rs = stat.executeQuery("SELECT * FROM reporting_data")) {
                    // Should not reach here
                }
            }, "reporting_data table should not exist in default datasource");
        }

        // Verify Flyway applied the correct number of migrations
        assertThat(flyway.info().all()).hasSize(1); // V1.0.0 from core
    }

    @Test
    @DisplayName("Reporting datasource should only have reporting migrations applied")
    public void testReportingDataSourceOnlyHasReportingMigrations() throws SQLException {
        // Verify reporting_data table exists (from reporting migrations)
        try (Connection connection = reportingDataSource.getConnection();
                Statement stat = connection.createStatement()) {
            try (ResultSet rs = stat.executeQuery("SELECT COUNT(*) FROM reporting_data")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).isEqualTo(0); // Should be empty (no INSERT migration)
            }

            // Verify core_operations table does NOT exist (should not be applied)
            assertThrows(JdbcSQLSyntaxErrorException.class, () -> {
                try (ResultSet rs = stat.executeQuery("SELECT * FROM core_operations")) {
                    // Should not reach here
                }
            }, "core_operations table should not exist in reporting datasource");
        }

        // Verify Flyway applied the correct number of migrations
        assertThat(flywayReporting.info().all()).hasSize(1); // Only V1.0.0 from reporting
    }

    @Test
    @DisplayName("Default datasource should have correct migration version")
    public void testDefaultDataSourceMigrationVersion() {
        String currentVersion = flyway.info().current().getVersion().toString();
        assertThat(currentVersion).isEqualTo("1.0.0");
    }

    @Test
    @DisplayName("Reporting datasource should have correct migration version")
    public void testReportingDataSourceMigrationVersion() {
        String currentVersion = flywayReporting.info().current().getVersion().toString();
        assertThat(currentVersion).isEqualTo("1.0.0");
    }
}
