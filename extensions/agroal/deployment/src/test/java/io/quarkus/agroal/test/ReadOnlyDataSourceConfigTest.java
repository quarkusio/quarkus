package io.quarkus.agroal.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.SQLException;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.agroal.api.AgroalDataSource;
import io.agroal.api.configuration.AgroalConnectionFactoryConfiguration;
import io.quarkus.test.QuarkusExtensionTest;

public class ReadOnlyDataSourceConfigTest {

    @Inject
    AgroalDataSource defaultDataSource;

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withConfigurationResource("base.properties")
            .overrideConfigKey("quarkus.datasource.jdbc.read-only", "true");

    @Test
    public void testReadOnlyDataSource() throws SQLException {
        AgroalConnectionFactoryConfiguration connectionFactoryConfiguration = defaultDataSource.getConfiguration()
                .connectionPoolConfiguration()
                .connectionFactoryConfiguration();

        // Quarkus wires quarkus.datasource.jdbc.read-only into Agroal's connection factory config.
        // Actual enforcement of read-only mode is driver-dependent (Connection.setReadOnly).
        assertThat(connectionFactoryConfiguration.readOnly()).isTrue();

        try (Connection connection = defaultDataSource.getConnection()) {
            assertThat(connection).isNotNull();
        }
    }
}
