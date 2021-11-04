package io.quarkus.flyway.test;

import static org.junit.jupiter.api.Assertions.assertThrows;

import javax.enterprise.inject.Instance;
import javax.enterprise.inject.UnsatisfiedResolutionException;
import javax.inject.Inject;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

/**
 * Flyway needs a datasource to work.
 * This tests assures, that an error occurs,
 * as soon as the default flyway configuration points to an missing default datasource.
 */
public class FlywayExtensionConfigEmptyTest {

    @Inject
    Instance<Flyway> flyway;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("config-empty.properties", "application.properties"));

    @Test
    @DisplayName("Injecting (default) flyway should fail if there is no datasource configured")
    public void testFlywayNotAvailableWithoutDataSource() {
        assertThrows(UnsatisfiedResolutionException.class, flyway::get);
    }
}
