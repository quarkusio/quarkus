package io.quarkus.flyway.test;

import static org.junit.jupiter.api.Assertions.assertThrows;

import javax.enterprise.inject.Instance;
import javax.enterprise.inject.UnsatisfiedResolutionException;
import javax.inject.Inject;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.flyway.FlywayDataSource;
import io.quarkus.test.QuarkusUnitTest;

/**
 * Flyway needs a datasource to work.
 * This tests assures, that an error occurs, as soon as a named flyway configuration points to an missing datasource.
 */
public class FlywayExtensionConfigMissingNamedDataSourceTest {

    @Inject
    @FlywayDataSource("users")
    Instance<Flyway> flyway;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("config-for-missing-named-datasource.properties", "application.properties"));

    @Test
    @DisplayName("Injecting flyway should fail if the named datasource is missing")
    public void testFlywayNotAvailableWithoutDataSource() {
        assertThrows(UnsatisfiedResolutionException.class, flyway::get);
    }
}
