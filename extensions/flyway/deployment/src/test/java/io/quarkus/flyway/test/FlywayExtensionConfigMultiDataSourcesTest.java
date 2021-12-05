package io.quarkus.flyway.test;

import static org.junit.jupiter.api.Assertions.assertFalse;

import javax.inject.Inject;
import javax.inject.Named;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.flyway.FlywayDataSource;
import io.quarkus.test.QuarkusUnitTest;

/**
 * Test a full configuration with default and two named datasources plus their flyway settings.
 */
public class FlywayExtensionConfigMultiDataSourcesTest {

    @Inject
    FlywayExtensionConfigFixture fixture;

    @Inject
    Flyway flyway;

    @Inject
    @FlywayDataSource("users")
    Flyway flywayUsers;

    @Inject
    @FlywayDataSource("inventory")
    Flyway flywayInventory;

    @Inject
    @Named("flyway_inventory")
    Flyway flywayNamedInventory;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(FlywayExtensionConfigFixture.class, FlywayExtensionCallback.class)
                    .addAsResource("config-for-multiple-datasources.properties", "application.properties"));

    @Test
    @DisplayName("Reads default flyway configuration for default datasource correctly")
    public void testFlywayDefaultConfigInjection() {
        fixture.assertAllConfigurationSettings(flyway.getConfiguration(), "");
        assertFalse(fixture.migrateAtStart(""));
    }

    @Test
    @DisplayName("Reads flyway configuration for datasource named 'users' correctly")
    public void testFlywayConfigNamedUsersInjection() {
        fixture.assertAllConfigurationSettings(flywayUsers.getConfiguration(), "users");
        assertFalse(fixture.migrateAtStart(""));
    }

    @Test
    @DisplayName("Reads flyway configuration for datasource named 'inventory' correctly")
    public void testFlywayConfigNamedInventoryInjection() {
        fixture.assertAllConfigurationSettings(flywayInventory.getConfiguration(), "inventory");
        assertFalse(fixture.migrateAtStart(""));
    }

    @Test
    @DisplayName("Reads flyway configuration directly named 'inventory_flyway' correctly")
    public void testFlywayConfigDirectlyNamedInventoryInjection() {
        fixture.assertAllConfigurationSettings(flywayNamedInventory.getConfiguration(), "inventory");
        assertFalse(fixture.migrateAtStart(""));
    }
}
