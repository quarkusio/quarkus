package io.quarkus.flyway.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import javax.inject.Inject;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.flyway.FlywayDataSource;
import io.quarkus.test.QuarkusUnitTest;

/**
 * Same as {@link FlywayExtensionMigrateAtStartTest} for named datasources.
 */
public class FlywayExtensionMigrateAtStartNamedDataSourceTest {

    @Inject
    @FlywayDataSource("users")
    Flyway flywayUsers;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("db/migration/V1.0.0__Quarkus.sql")
                    .addAsResource("migrate-at-start-config-named-datasource.properties", "application.properties"));

    @Test
    @DisplayName("Migrates at start for datasource named 'users' correctly")
    public void testFlywayConfigInjection() {
        MigrationInfo migrationInfo = flywayUsers.info().current();
        assertNotNull(migrationInfo, "No Flyway migration was executed");

        String currentVersion = migrationInfo
                .getVersion()
                .toString();
        // Expected to be 1.0.0 as migration runs at start
        assertEquals("1.0.0", currentVersion);
    }
}
