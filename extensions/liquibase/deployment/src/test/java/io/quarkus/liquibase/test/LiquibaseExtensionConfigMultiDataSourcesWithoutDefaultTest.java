package io.quarkus.liquibase.test;

import static org.junit.jupiter.api.Assertions.assertFalse;

import javax.inject.Inject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.liquibase.LiquibaseDataSource;
import io.quarkus.liquibase.LiquibaseFactory;
import io.quarkus.test.QuarkusUnitTest;

/**
 * Test a full configuration with default and two named datasources plus their liquibase settings.
 */
public class LiquibaseExtensionConfigMultiDataSourcesWithoutDefaultTest {

    @Inject
    LiquibaseExtensionConfigFixture fixture;

    @Inject
    @LiquibaseDataSource("users")
    LiquibaseFactory liquibaseUsers;

    @Inject
    @LiquibaseDataSource("inventory")
    LiquibaseFactory liquibaseInventory;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(LiquibaseExtensionConfigFixture.class)
                    .addAsResource("db/changeLog.xml")
                    .addAsResource("db/inventory/changeLog.xml")
                    .addAsResource("db/users/changeLog.xml")
                    .addAsResource("config-for-multiple-datasources-without-default.properties", "application.properties"));

    @Test
    @DisplayName("Reads liquibase configuration for datasource named 'users' without default datasource correctly")
    public void testLiquibaseConfigNamedUsersInjection() {
        fixture.assertAllConfigurationSettings(liquibaseUsers.getConfiguration(), "users");
        assertFalse(fixture.migrateAtStart(""));
    }

    @Test
    @DisplayName("Reads liquibase configuration for datasource named 'inventory' without default datasource correctly")
    public void tesLiquibaseConfigNamedInventoryInjection() {
        fixture.assertAllConfigurationSettings(liquibaseInventory.getConfiguration(), "inventory");
        assertFalse(fixture.migrateAtStart(""));
    }
}
