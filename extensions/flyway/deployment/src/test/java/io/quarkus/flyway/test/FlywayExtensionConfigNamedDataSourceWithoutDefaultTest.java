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
public class FlywayExtensionConfigNamedDataSourceWithoutDefaultTest {

    @Inject
    FlywayExtensionConfigFixture fixture;

    @Inject
    @FlywayDataSource("users")
    Flyway flywayUsers;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(FlywayExtensionConfigFixture.class, FlywayExtensionCallback.class)
                    .addAsResource("config-for-named-datasource-without-default.properties", "application.properties"));

    @Test
    @DisplayName("Reads flyway configuration for datasource named 'users' without default datasource correctly")
    public void testFlywayConfigNamedUsersInjection() {
        fixture.assertAllConfigurationSettings(flywayUsers.getConfiguration(), "users");
        assertFalse(fixture.migrateAtStart(""));
    }
}
