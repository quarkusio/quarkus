package io.quarkus.agroal.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.agroal.api.AgroalDataSource;
import io.quarkus.agroal.DataSource;
import io.quarkus.agroal.runtime.DataSourcesJdbcRuntimeConfig;
import io.quarkus.agroal.runtime.UnconfiguredDataSource;
import io.quarkus.arc.Arc;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.test.QuarkusUnitTest;

/**
 * Test that Agroal is able to install a named datasource,
 * even when there is no runtime configuration for that datasource.
 * <p>
 * This case is interesting mainly because it would most likely lead to an NPE
 * unless we take extra care to avoid it.
 */
public class NoRuntimeConfigNamedDataSourceTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            // For Agroal, provide only build-time config; rely on defaults for everything else.
            .overrideConfigKey("quarkus.datasource.\"ds-1\".db-kind", "h2")
            // Disable dev services, since they could interfere with runtime config (in particular the JDBC URL setting).
            .overrideConfigKey("quarkus.devservices.enabled", "false");

    @Test
    public void test() {
        // Check that runtime config for our datasource is indeed missing.
        DataSourcesJdbcRuntimeConfig config = Arc.container().instance(DataSourcesJdbcRuntimeConfig.class).get();
        assertThat(config.namedDataSources).isNullOrEmpty();
        // Test that the datasource was installed correctly regardless.
        AgroalDataSource dataSource = Arc.container()
                .instance(AgroalDataSource.class, new DataSource.DataSourceLiteral("ds-1")).get();
        assertThat(dataSource).isNotNull();
        // Though of course, the datasource cannot be used.
        assertThat(dataSource).isInstanceOf(UnconfiguredDataSource.class);
        assertThatThrownBy(() -> {
            try (Connection connection = dataSource.getConnection()) {
            }
        })
                .isInstanceOf(ConfigurationException.class)
                .hasMessage("quarkus.datasource.ds-1.jdbc.url has not been defined");
    }
}
