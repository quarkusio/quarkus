package io.quarkus.agroal.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.sql.Connection;
import java.sql.SQLException;

import io.agroal.api.AgroalDataSource;
import io.agroal.api.configuration.AgroalConnectionPoolConfiguration;

final class MultipleDataSourcesTestUtil {

    private MultipleDataSourcesTestUtil() {
    }

    static void testDataSource(String dataSourceName, AgroalDataSource dataSource, String jdbcUrl, String username,
            int maxSize)
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
        assertEquals(maxSize, configuration.maxSize());

        try (Connection connection = dataSource.getConnection()) {
        }
    }
}
