package io.quarkus.jdbc.runtime.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLException;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.agroal.api.AgroalDataSource;
import io.quarkus.agroal.DataSource;
import io.quarkus.test.QuarkusExtensionTest;

public class RuntimeResolvedDriverConfigTest {

    @Inject
    AgroalDataSource defaultDataSource;

    @Inject
    @DataSource("users")
    AgroalDataSource usersDataSource;

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withConfigurationResource("application-runtime-resolved-driver.properties");

    @Test
    public void defaultDataSourceUsesRuntimeResolvedDriver() throws SQLException {
        final var poolConfiguration = defaultDataSource.getConfiguration().connectionPoolConfiguration();
        final var connectionFactoryConfiguration = poolConfiguration.connectionFactoryConfiguration();

        assertThat(connectionFactoryConfiguration.connectionProviderClass().getName()).isEqualTo("org.h2.Driver");
        assertThat(connectionFactoryConfiguration.jdbcUrl()).isEqualTo("jdbc:h2:mem:runtime-default");
        assertThat(connectionFactoryConfiguration.principal().getName()).isEqualTo("username-default");
        assertThat(poolConfiguration.minSize()).isEqualTo(3);
        assertThat(poolConfiguration.maxSize()).isEqualTo(13);
        assertThat(poolConfiguration.initialSize()).isEqualTo(7);

        assertSelectOne(defaultDataSource);
    }

    @Test
    public void namedDataSourceUsesRuntimeResolvedDriver() throws SQLException {
        final var connectionFactoryConfiguration = usersDataSource.getConfiguration().connectionPoolConfiguration()
                .connectionFactoryConfiguration();

        assertThat(connectionFactoryConfiguration.connectionProviderClass().getName()).isEqualTo("org.h2.Driver");
        assertThat(connectionFactoryConfiguration.jdbcUrl()).isEqualTo("jdbc:h2:mem:runtime-users");
        assertThat(connectionFactoryConfiguration.principal().getName()).isEqualTo("username-users");

        assertSelectOne(usersDataSource);
    }

    private static void assertSelectOne(final AgroalDataSource dataSource) throws SQLException {
        try (final var connection = dataSource.getConnection();
                final var statement = connection.createStatement();
                final var resultSet = statement.executeQuery("SELECT 1")) {
            assertThat(resultSet.next()).isTrue();
            assertThat(resultSet.getInt(1)).isEqualTo(1);
        }
    }
}
