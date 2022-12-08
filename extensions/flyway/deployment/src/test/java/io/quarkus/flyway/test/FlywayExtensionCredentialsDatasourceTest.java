package io.quarkus.flyway.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.inject.Inject;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class FlywayExtensionCredentialsDatasourceTest {

    @Inject
    Flyway flyway;

    @Inject
    FlywayExtensionConfigFixture fixture;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(FlywayExtensionConfigFixture.class)
                    .addClasses(FlywayExtensionCallback.class, FlywayExtensionCallback2.class)
                    .addAsResource("config-for-credentials-datasource.properties", "application.properties"));

    @Test
    @DisplayName("Reads flyway configuration for credentials datasource correctly")
    public void testFlywayConfigInjection() {
        assertEquals("sa", fixture.username(""), "Username config not found!");
        assertEquals("sa", fixture.password(""), "Password config not found!");
        assertEquals("jdbc:h2:mem:flyway_db", fixture.jdbcUrl(""), "jdbc-url config not found!");
    }

}
