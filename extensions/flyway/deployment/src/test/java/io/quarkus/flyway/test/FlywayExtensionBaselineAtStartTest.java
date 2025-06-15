package io.quarkus.flyway.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.inject.Inject;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class FlywayExtensionBaselineAtStartTest {
    @Inject
    Flyway flyway;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addAsResource("db/migration/V1.0.0__Quarkus.sql")
                    .addAsResource("baseline-at-start-config.properties", "application.properties"));

    @Test
    @DisplayName("Baseline at start is executed against empty schema")
    public void testFlywayConfigInjection() {
        MigrationInfo migrationInfo = flyway.info().current();
        assertNotNull(migrationInfo, "No Flyway migration was executed");
        assertTrue(migrationInfo.getType().isBaseline(), "Flyway migration is not a baseline");
        String currentVersion = migrationInfo.getVersion().toString();

        assertEquals("1.0.1", currentVersion);
    }
}
