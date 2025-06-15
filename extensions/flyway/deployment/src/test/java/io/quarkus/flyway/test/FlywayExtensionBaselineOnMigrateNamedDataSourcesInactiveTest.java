package io.quarkus.flyway.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.flyway.FlywayDataSource;
import io.quarkus.test.QuarkusUnitTest;

public class FlywayExtensionBaselineOnMigrateNamedDataSourcesInactiveTest {

    @Inject
    @FlywayDataSource("users")
    Flyway flywayUsers;

    @Inject
    @FlywayDataSource("laptops")
    Flyway flywayLaptops;

    static final FlywayH2TestCustomizer customizerUsers = FlywayH2TestCustomizer
            .withDbName("quarkus-flyway-baseline-on-named-ds-users").withPort(11302)
            .withInitSqlFile("src/test/resources/h2-init-data.sql");

    static final FlywayH2TestCustomizer customizerLaptops = FlywayH2TestCustomizer
            .withDbName("quarkus-flyway-baseline-on-named-ds-laptops").withPort(11303)
            .withInitSqlFile("src/test/resources/h2-init-data.sql");

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest().setBeforeAllCustomizer(new Runnable() {
        @Override
        public void run() {
            customizerUsers.startH2();
            customizerLaptops.startH2();
        }
    }).setAfterAllCustomizer(new Runnable() {
        @Override
        public void run() {
            customizerUsers.stopH2();
            customizerLaptops.stopH2();
        }
    }).withApplicationRoot((jar) -> jar.addClass(FlywayH2TestCustomizer.class)
            .addAsResource("baseline-on-migrate-named-datasources-inactive.properties", "application.properties"));

    @Test
    @DisplayName("Create history table correctly")
    public void testFlywayInitialBaselineInfo() {
        MigrationInfo baselineInfo = flywayUsers.info().applied()[0];

        assertEquals("0.0.1", baselineInfo.getVersion().getVersion());
        assertEquals("Initial description for test", baselineInfo.getDescription());
    }

    @Test
    @DisplayName("History table not created if inactive")
    public void testFlywayInitialBaselineInfoInactive() {
        assertEquals(0, flywayLaptops.info().applied().length);
    }
}
