package io.quarkus.agroal.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.SQLException;
import java.time.Duration;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.agroal.api.AgroalDataSource;
import io.agroal.api.configuration.AgroalConnectionFactoryConfiguration;
import io.agroal.api.configuration.AgroalConnectionPoolConfiguration;
import io.agroal.narayana.NarayanaTransactionIntegration;
import io.quarkus.test.QuarkusUnitTest;

public class GcpDataSourceConfigTest {

    //tag::injection[]
    @Inject
    AgroalDataSource defaultDataSource;
    //end::injection[]

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withConfigurationResource("application-gcp-datasource.properties");

    @Test
    public void testGCPDataSourceInjection() throws SQLException {
        testDataSource(defaultDataSource, "username-default", 3, 13, 7, Duration.ofSeconds(53), Duration.ofSeconds(54),
                Duration.ofSeconds(55), Duration.ofSeconds(56), Duration.ofSeconds(57),
                "create schema if not exists schema_default");
    }

    private static void testDataSource(AgroalDataSource dataSource, String username, int minSize, int maxSize,
            int initialSize, Duration backgroundValidationInterval, Duration acquisitionTimeout, Duration leakDetectionInterval,
            Duration idleRemovalInterval, Duration maxLifetime, String newConnectionSql) {
        AgroalConnectionPoolConfiguration configuration = dataSource.getConfiguration().connectionPoolConfiguration();
        AgroalConnectionFactoryConfiguration agroalConnectionFactoryConfiguration = configuration
                .connectionFactoryConfiguration();

        assertTrue(agroalConnectionFactoryConfiguration.jdbcProperties().containsKey("socketFactory"));
        assertEquals(agroalConnectionFactoryConfiguration.jdbcProperties().getProperty("socketFactory"),
                "com.google.cloud.sql.postgres.SocketFactory");
        assertTrue(agroalConnectionFactoryConfiguration.jdbcProperties().containsKey("randomProperty"));
        assertEquals(agroalConnectionFactoryConfiguration.jdbcProperties().getProperty("randomProperty"), "myProperty");
        assertTrue(agroalConnectionFactoryConfiguration.jdbcProperties().containsKey("cloudSqlInstance"));
        assertEquals(agroalConnectionFactoryConfiguration.jdbcProperties().getProperty("cloudSqlInstance"),
                "project:zone:db-name");
        assertEquals(username, agroalConnectionFactoryConfiguration.principal().getName());
        assertEquals(minSize, configuration.minSize());
        assertEquals(maxSize, configuration.maxSize());
        assertEquals(initialSize, configuration.initialSize());
        assertEquals(backgroundValidationInterval, configuration.validationTimeout());
        assertEquals(acquisitionTimeout, configuration.acquisitionTimeout());
        assertEquals(leakDetectionInterval, configuration.leakTimeout());
        assertEquals(idleRemovalInterval, configuration.reapTimeout());
        assertEquals(maxLifetime, configuration.maxLifetime());
        assertTrue(configuration.transactionIntegration() instanceof NarayanaTransactionIntegration);
        assertEquals(AgroalConnectionFactoryConfiguration.TransactionIsolation.SERIALIZABLE,
                agroalConnectionFactoryConfiguration.jdbcTransactionIsolation());
        assertTrue(agroalConnectionFactoryConfiguration.trackJdbcResources());
        assertTrue(dataSource.getConfiguration().metricsEnabled());
        assertEquals(newConnectionSql, agroalConnectionFactoryConfiguration.initialSql());
    }
}
