package io.quarkus.mongodb;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.mongodb.client.MongoClient;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.quarkus.arc.Arc;
import io.quarkus.mongodb.reactive.ReactiveMongoClient;
import io.quarkus.test.QuarkusUnitTest;

/** Variation of {@link io.quarkus.mongodb.MongoMetricsTest} to verify lazy client initialization. */
class MongoLazyTest extends MongoTestBase {

    @Inject
    MeterRegistry meterRegistry;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClasses(MongoTestBase.class))
            .withConfigurationResource("application-metrics-mongo.properties");

    @Test
    void testLazyClientCreation() {
        // Clients are created lazily, this metric should not be present yet
        assertThat(getMetric("mongodb.driver.pool.size")).isNull();
        assertThat(getMetric("mongodb.driver.pool.checkedout")).isNull();
        assertThat(getMetric("mongodb.driver.commands")).isNull();

        // doing this here instead of in another method in order to avoid messing with the initialization stats
        assertThat(Arc.container().instance(MongoClient.class).get()).isNull();
        assertThat(Arc.container().instance(ReactiveMongoClient.class).get()).isNull();
    }

    private Double getMetric(String name) {
        Meter metric = meterRegistry.getMeters()
                .stream()
                .filter(mtr -> mtr.getId().getName().contains(name))
                .findFirst()
                .orElse(null);
        return metric == null ? null : metric.measure().iterator().next().getValue();
    }

}
