package io.quarkus.flyway.test;

import static org.junit.jupiter.api.Assertions.assertNull;

import jakarta.inject.Inject;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class FlywayExtensionBaselineAtStartExistingSchemaHistoryTableTest {
    @Inject
    Flyway flyway;

    static final FlywayH2TestCustomizer customizer = FlywayH2TestCustomizer
            .withDbName("quarkus-baseline-at-start-existing-schema-history")
            .withPort(11309)
            .withInitSqlFile("src/test/resources/h2-init-schema-history-table.sql");

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setBeforeAllCustomizer(customizer::startH2)
            .setAfterAllCustomizer(customizer::stopH2)
            .withApplicationRoot((jar) -> jar
                    .addClass(FlywayH2TestCustomizer.class)
                    .addAsResource("db/migration/V1.0.0__Quarkus.sql")
                    .addAsResource("baseline-at-start-existing-schema-history-table-config.properties",
                            "application.properties"));

    @Test
    @DisplayName("Baseline at start is not executed against existing schema-history-table")
    public void testFlywayConfigInjection() {
        MigrationInfo migrationInfo = flyway.info().current();
        assertNull(migrationInfo, "Flyway baseline was executed on existing schema history table");
    }
}
