package io.quarkus.flyway.test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.UnsatisfiedResolutionException;
import jakarta.inject.Inject;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.flyway.FlywayDataSource;
import io.quarkus.test.QuarkusUnitTest;

public class FlywayExtensionConfigEmptyNamedDataSourceTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            // We need this otherwise the *default* datasource may impact this test
            .overrideConfigKey("quarkus.datasource.db-kind", "h2")
            .overrideConfigKey("quarkus.datasource.username", "sa")
            .overrideConfigKey("quarkus.datasource.password", "sa")
            .overrideConfigKey("quarkus.datasource.jdbc.url",
                    "jdbc:h2:tcp://localhost/mem:test-quarkus-migrate-at-start;DB_CLOSE_DELAY=-1")
            // The datasource won't be truly "unconfigured" if dev services are enabled
            .overrideConfigKey("quarkus.devservices.enabled", "false");

    @Inject
    @FlywayDataSource("users")
    Instance<Flyway> flywayForNamedDatasource;

    @Test
    @DisplayName("If there is no config for a named datasource, the application should boot, but Flyway should be deactivated for that datasource")
    public void testBootSucceedsButFlywayDeactivated() {
        assertThatThrownBy(flywayForNamedDatasource::get)
                .isInstanceOf(UnsatisfiedResolutionException.class)
                .hasMessageContaining("No bean found");
    }
}
