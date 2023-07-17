package io.quarkus.agroal.test;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.agroal.api.AgroalDataSource;
import io.quarkus.test.QuarkusUnitTest;

public class PoolLessTestCase {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest().withConfigurationResource("base.properties")
            .overrideConfigKey("quarkus.datasource.jdbc.enable-metrics", "true")
            .overrideConfigKey("quarkus.datasource.jdbc.pooling-enabled", "false");

    @Inject
    AgroalDataSource defaultDS;

    @Test
    public void testConnectionDisposal() throws SQLException {
        try (Connection connection = defaultDS.getConnection()) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("SELECT 1");
            }
        }

        // Assert there are no connections in the pool
        Assertions.assertEquals(0L, defaultDS.getMetrics().activeCount());

        Assertions.assertEquals(1L, defaultDS.getMetrics().creationCount());
        Assertions.assertEquals(1L, defaultDS.getMetrics().acquireCount());
        Assertions.assertEquals(1L, defaultDS.getMetrics().flushCount());
        Assertions.assertEquals(1L, defaultDS.getMetrics().destroyCount());
    }
}
