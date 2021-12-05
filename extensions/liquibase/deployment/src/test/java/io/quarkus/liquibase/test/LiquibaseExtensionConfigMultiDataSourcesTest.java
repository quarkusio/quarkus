package io.quarkus.liquibase.test;

import static org.junit.jupiter.api.Assertions.assertFalse;

import javax.inject.Inject;
import javax.inject.Named;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.liquibase.LiquibaseDataSource;
import io.quarkus.liquibase.LiquibaseFactory;
import io.quarkus.test.QuarkusUnitTest;

/**
 * Test a full configuration with default and two named datasources plus their liquibase settings.
 */
public class LiquibaseExtensionConfigMultiDataSourcesTest {

    @Inject
    LiquibaseExtensionConfigFixture fixture;

    @Inject
    LiquibaseFactory liquibase;

    @Inject
    @LiquibaseDataSource("users")
    LiquibaseFactory liquibaseUsers;

    @Inject
    @LiquibaseDataSource("inventory")
    LiquibaseFactory liquibaseInventory;

    @Inject
    @Named("liquibase_inventory")
    LiquibaseFactory liquibaseNamedInventory;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(LiquibaseExtensionConfigFixture.class)
                    .addAsResource("db/inventory/changeLog.xml")
                    .addAsResource("db/users/changeLog.xml")
                    .addAsResource("db/xml/changeLog.xml")
                    .addAsResource("db/xml/create-tables.xml")
                    .addAsResource("db/xml/create-views.xml")
                    .addAsResource("db/xml/test/test.xml")
                    .addAsResource("config-for-multiple-datasources.properties", "application.properties"));

    @Test
    @DisplayName("Reads default liquibase configuration for default datasource correctly")
    public void testLiquibaseDefaultConfigInjection() {
        fixture.assertAllConfigurationSettings(liquibase.getConfiguration(), "");
        assertFalse(fixture.migrateAtStart(""));
    }

    @Test
    @DisplayName("Reads liquibase configuration for datasource named 'users' correctly")
    public void testLiquibaseConfigNamedUsersInjection() {
        fixture.assertAllConfigurationSettings(liquibaseUsers.getConfiguration(), "users");
        assertFalse(fixture.migrateAtStart(""));
    }

    @Test
    @DisplayName("Reads liquibase configuration for datasource named 'inventory' correctly")
    public void testLiquibaseConfigNamedInventoryInjection() {
        fixture.assertAllConfigurationSettings(liquibaseInventory.getConfiguration(), "inventory");
        assertFalse(fixture.migrateAtStart(""));
    }

    @Test
    @DisplayName("Reads liquibase configuration directly named 'liquibase_inventory' correctly")
    public void testLiquibaseConfigDirectlyNamedInventoryInjection() {
        fixture.assertAllConfigurationSettings(liquibaseNamedInventory.getConfiguration(), "inventory");
        assertFalse(fixture.migrateAtStart(""));
    }
}
