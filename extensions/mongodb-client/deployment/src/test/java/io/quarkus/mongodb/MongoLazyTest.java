package io.quarkus.mongodb;

import static org.assertj.core.api.Assertions.assertThat;

import javax.inject.Inject;

import org.eclipse.microprofile.metrics.Metric;
import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Tag;
import org.eclipse.microprofile.metrics.annotation.RegistryType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.mongodb.client.MongoClient;

import io.quarkus.arc.Arc;
import io.quarkus.mongodb.metrics.ConnectionPoolGauge;
import io.quarkus.mongodb.reactive.ReactiveMongoClient;
import io.quarkus.test.QuarkusUnitTest;

/** Variation of {@link io.quarkus.mongodb.MongoMetricsTest} to verify lazy client initialization. */
public class MongoLazyTest extends MongoTestBase {

    @Inject
    @RegistryType(type = MetricRegistry.Type.VENDOR)
    MetricRegistry registry;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(MongoTestBase.class))
            .withConfigurationResource("application-metrics-mongo.properties");

    @Test
    void testLazyClientCreation() {
        // Clients are created lazily, this metric should not be present yet
        assertThat(getGaugeValueOrNull("mongodb.connection-pool.size", getTags())).isNull();
        assertThat(getGaugeValueOrNull("mongodb.connection-pool.checked-out-count", getTags())).isNull();

        // doing this here instead of in another method in order to avoid messing with the initialization stats
        assertThat(Arc.container().instance(MongoClient.class).get()).isNull();
        assertThat(Arc.container().instance(ReactiveMongoClient.class).get()).isNull();
    }

    private Long getGaugeValueOrNull(String metricName, Tag[] tags) {
        MetricID metricID = new MetricID(metricName, tags);
        Metric metric = registry.getMetrics().get(metricID);

        if (metric == null) {
            return null;
        }
        return ((ConnectionPoolGauge) metric).getValue();
    }

    private Tag[] getTags() {
        return new Tag[] {
                new Tag("host", "127.0.0.1"),
                new Tag("port", "27018"),
        };
    }
}
