package io.quarkus.flyway.test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class FlywayExtensionDisabledTest {

    @Inject
    Instance<Flyway> flyway;

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("db/migration/V1.0.0__Quarkus.sql")
                    .addAsResource("disabled-config.properties", "application.properties"));

    @Test
    @DisplayName("No Flyway instance available if disabled")
    public void testFlywayConfigInjection() {
        assertTrue(flyway.isUnsatisfied());
    }
}
