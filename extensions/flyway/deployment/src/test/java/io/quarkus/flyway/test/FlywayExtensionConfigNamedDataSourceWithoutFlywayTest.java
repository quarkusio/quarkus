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
 * Assures, that flyway can also be used without any configuration,
 * provided, that at least a named datasource is configured.
 */
public class FlywayExtensionConfigNamedDataSourceWithoutFlywayTest {

    @Inject
    @FlywayDataSource("users")
    Flyway flyway;

    @Inject
    FlywayExtensionConfigFixture fixture;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(FlywayExtensionConfigFixture.class)
                    .addAsResource("config-for-named-datasource-without-flyway.properties", "application.properties"));

    @Test
    @DisplayName("Reads predefined default flyway configuration for named datasource correctly")
    public void testFlywayDefaultConfigInjection() {
        fixture.assertDefaultConfigurationSettings(flyway.getConfiguration());
        assertFalse(fixture.migrateAtStart("users"));
    }
}
