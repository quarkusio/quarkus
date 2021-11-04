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
public class LiquibaseExtensionConfigNamedDataSourceWithoutDefaultTest {

    @Inject
    LiquibaseExtensionConfigFixture fixture;

    @Inject
    @LiquibaseDataSource("users")
    LiquibaseFactory liquibaseUsers;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(LiquibaseExtensionConfigFixture.class)
                    .addAsResource("db/xml/changeLog.xml")
                    .addAsResource("db/xml/create-tables.xml")
                    .addAsResource("db/xml/create-views.xml")
                    .addAsResource("db/xml/test/test.xml")
                    .addAsResource("config-for-named-datasource-without-default.properties", "application.properties"));

    @Test
    @DisplayName("Reads liquibase configuration for datasource named 'users' without default datasource correctly")
    public void testLiquibaseConfigNamedUsersInjection() {
        fixture.assertAllConfigurationSettings(liquibaseUsers.getConfiguration(), "users");
        assertFalse(fixture.migrateAtStart(""));
    }
}
