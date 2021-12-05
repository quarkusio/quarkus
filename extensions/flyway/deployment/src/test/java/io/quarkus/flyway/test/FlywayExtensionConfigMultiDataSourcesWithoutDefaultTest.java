package io.quarkus.flyway.test;

import static org.junit.jupiter.api.Assertions.assertFalse;

import javax.inject.Inject;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.flyway.FlywayDataSource;
import io.quarkus.test.QuarkusUnitTest;

/**
 * Test a full configuration with default and two named datasources plus their flyway settings.
 */
public class FlywayExtensionConfigMultiDataSourcesWithoutDefaultTest {

    @Inject
    FlywayExtensionConfigFixture fixture;

    @Inject
    @FlywayDataSource("users")
    Flyway flywayUsers;

    @Inject
    @FlywayDataSource("inventory")
    Flyway flywayInventory;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(FlywayExtensionConfigFixture.class, FlywayExtensionCallback.class)
                    .addAsResource("config-for-multiple-datasources-without-default.properties", "application.properties"));

    @Test
    @DisplayName("Reads flyway configuration for datasource named 'users' without default datasource correctly")
    public void testFlywayConfigNamedUsersInjection() {
        fixture.assertAllConfigurationSettings(flywayUsers.getConfiguration(), "users");
        assertFalse(fixture.migrateAtStart(""));
    }

    @Test
    @DisplayName("Reads flyway configuration for datasource named 'inventory' without default datasource correctly")
    public void testFlywayConfigNamedInventoryInjection() {
        fixture.assertAllConfigurationSettings(flywayInventory.getConfiguration(), "inventory");
        assertFalse(fixture.migrateAtStart(""));
    }
}
