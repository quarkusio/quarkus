package io.quarkus.mongodb;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.mongodb.client.MongoClient;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.quarkus.arc.Arc;
import io.quarkus.mongodb.reactive.ReactiveMongoClient;
import io.quarkus.test.QuarkusUnitTest;

class MongoMetricsTest extends MongoTestBase {

    @Inject
    MongoClient client;

    @Inject
    MeterRegistry meterRegistry;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClasses(MongoTestBase.class))
            .withConfigurationResource("application-metrics-mongo.properties");

    @AfterEach
    void cleanup() {
        if (client != null) {
            client.close();
        }
    }

    @Test
    void testMetricsInitialization() {
        // Clients are created lazily, this metric should not be present yet
        assertThat(getMetric("mongodb.driver.pool.size")).isNull();
        assertThat(getMetric("mongodb.driver.pool.checkedout")).isNull();

        // Just need to execute something so that a connection is opened
        String name = client.listDatabaseNames().first();

        assertThat(getMetric("mongodb.driver.pool.size")).isOne();
        assertThat(getMetric("mongodb.driver.commands")).isOne();
        assertThat(getMetric("mongodb.driver.pool.checkedout")).isZero();

        client.close();
        assertThat(getMetric("mongodb.driver.pool.size")).isNull();
        assertThat(getMetric("mongodb.driver.pool.checkedout")).isNull();

        // doing this here instead of in another method in order to avoid messing with the initialization stats
        assertThat(Arc.container().instance(MongoClient.class).get()).isNotNull();
        assertThat(Arc.container().instance(ReactiveMongoClient.class).get()).isNull();
    }

    private Double getMetric(String metricName) {
        Meter metric = meterRegistry.getMeters()
                .stream()
                .filter(mtr -> mtr.getId().getName().contains(metricName))
                .findFirst()
                .orElse(null);
        return metric == null ? null : metric.measure().iterator().next().getValue();
    }

}
