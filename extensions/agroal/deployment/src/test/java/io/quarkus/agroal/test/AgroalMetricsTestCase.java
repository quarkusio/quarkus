package io.quarkus.agroal.test;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import jakarta.inject.Inject;

import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Tag;
import org.eclipse.microprofile.metrics.annotation.RegistryType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.agroal.api.AgroalDataSource;
import io.quarkus.agroal.DataSource;
import io.quarkus.test.QuarkusUnitTest;

public class AgroalMetricsTestCase {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withConfigurationResource("application-metrics-enabled.properties");

    @Inject
    AgroalDataSource defaultDS;

    @Inject
    @DataSource("ds1")
    AgroalDataSource ds1;

    @Inject
    @RegistryType(type = MetricRegistry.Type.VENDOR)
    MetricRegistry registry;

    @Test
    public void testMetricsOfDefaultDS() throws SQLException {
        Counter acquireCount = registry.getCounters()
                .get(new MetricID("agroal.acquire.count", new Tag("datasource", "default")));
        Gauge<?> maxUsed = registry.getGauges()
                .get(new MetricID("agroal.max.used.count", new Tag("datasource", "default")));

        Assertions.assertNotNull(acquireCount, "Agroal metrics should be registered eagerly");
        Assertions.assertNotNull(maxUsed, "Agroal metrics should be registered eagerly");

        try (Connection connection = defaultDS.getConnection()) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("SELECT 1");
            }
        }

        Assertions.assertEquals(1L, acquireCount.getCount());
        Assertions.assertEquals(1L, maxUsed.getValue());
    }

    @Test
    public void testMetricsOfDs1() throws SQLException {
        Counter acquireCount = registry.getCounters().get(new MetricID("agroal.acquire.count",
                new Tag("datasource", "ds1")));
        Gauge<?> maxUsed = registry.getGauges().get(new MetricID("agroal.max.used.count",
                new Tag("datasource", "ds1")));
        Assertions.assertNotNull(acquireCount, "Agroal metrics should be registered eagerly");
        Assertions.assertNotNull(maxUsed, "Agroal metrics should be registered eagerly");

        try (Connection connection = ds1.getConnection()) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("SELECT 1");
            }
        }

        Assertions.assertEquals(1L, acquireCount.getCount());
        Assertions.assertEquals(1L, maxUsed.getValue());
    }

}
