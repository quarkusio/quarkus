package io.quarkus.jdbc.runtime.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLException;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.agroal.api.AgroalDataSource;
import io.quarkus.test.QuarkusExtensionTest;

public class RuntimeResolvedXaDriverTest {

    @Inject
    AgroalDataSource dataSource;

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .overrideConfigKey("quarkus.datasource.db-kind", "runtime")
            .overrideConfigKey("quarkus.datasource.jdbc.transactions", "xa")
            .overrideRuntimeConfigKey("quarkus.datasource.jdbc.url", "jdbc:h2:mem:runtime-xa")
            .overrideRuntimeConfigKey("quarkus.datasource.jdbc-runtime.driver", "org.h2.jdbcx.JdbcDataSource");

    @Test
    public void xaDataSourceIsRuntimeResolved() throws SQLException {
        final var connectionFactoryConfiguration = dataSource.getConfiguration().connectionPoolConfiguration()
                .connectionFactoryConfiguration();
        assertThat(connectionFactoryConfiguration.connectionProviderClass().getName())
                .isEqualTo("org.h2.jdbcx.JdbcDataSource");

        try (final var connection = dataSource.getConnection();
                final var statement = connection.createStatement();
                final var resultSet = statement.executeQuery("SELECT 1")) {
            assertThat(resultSet.next()).isTrue();
            assertThat(resultSet.getInt(1)).isEqualTo(1);
        }
    }
}
