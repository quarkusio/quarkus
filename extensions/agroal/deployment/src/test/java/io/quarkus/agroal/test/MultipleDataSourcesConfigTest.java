package io.quarkus.agroal.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.sql.Connection;
import java.sql.SQLException;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.agroal.api.AgroalDataSource;
import io.agroal.api.configuration.AgroalConnectionPoolConfiguration;
import io.quarkus.agroal.DataSource;
import io.quarkus.test.QuarkusUnitTest;

public class MultipleDataSourcesConfigTest {

    //tag::injection[]
    @Inject
    AgroalDataSource defaultDataSource;

    @Inject
    @DataSource("users")
    AgroalDataSource dataSource1;

    @Inject
    @DataSource("inventory")
    AgroalDataSource dataSource2;
    //end::injection[]

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withConfigurationResource("application-multiple-datasources.properties");

    @Test
    public void testDataSourceInjection() throws SQLException {
        testDataSource("default", defaultDataSource, "jdbc:h2:tcp://localhost/mem:default", "username-default", 3, 13);
        testDataSource("users", dataSource1, "jdbc:h2:tcp://localhost/mem:users", "username1", 1, 11);
        testDataSource("inventory", dataSource2, "jdbc:h2:tcp://localhost/mem:inventory", "username2", 2, 12);
    }

    private static void testDataSource(String dataSourceName, AgroalDataSource dataSource, String jdbcUrl, String username,
            int minSize, int maxSize)
            throws SQLException {
        AgroalConnectionPoolConfiguration configuration = null;

        try {
            configuration = dataSource.getConfiguration().connectionPoolConfiguration();
        } catch (NullPointerException e) {
            // we catch the NPE here as we have a proxy and we can't test dataSource directly
            fail("Datasource " + dataSourceName + " should not be null");
        }
        assertEquals(jdbcUrl, configuration.connectionFactoryConfiguration().jdbcUrl());
        assertEquals(username, configuration.connectionFactoryConfiguration().principal().getName());
        assertEquals(minSize, configuration.minSize());
        assertEquals(maxSize, configuration.maxSize());

        try (Connection connection = dataSource.getConnection()) {
        }
    }
}
