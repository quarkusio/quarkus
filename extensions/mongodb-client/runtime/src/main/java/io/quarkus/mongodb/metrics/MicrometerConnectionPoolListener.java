package io.quarkus.mongodb.metrics;

import com.mongodb.event.ConnectionPoolCreatedEvent;
import com.mongodb.event.ConnectionPoolListener;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.mongodb.DefaultMongoConnectionPoolTagsProvider;
import io.micrometer.core.instrument.binder.mongodb.MongoConnectionPoolTagsProvider;
import io.micrometer.core.instrument.binder.mongodb.MongoMetricsConnectionPoolListener;

public class MicrometerConnectionPoolListener {
    public static ConnectionPoolListener createMicrometerConnectionPool() {
        return new MongoMetricsConnectionPoolListener(Metrics.globalRegistry);
    }

    public static ConnectionPoolListener createMicrometerConnectionPool(final String clientName) {
        return new MongoMetricsConnectionPoolListener(Metrics.globalRegistry,
                new MongoConnectionPoolTagsProvider() {
                    @Override
                    public Iterable<Tag> connectionPoolTags(ConnectionPoolCreatedEvent event) {
                        return Tags.of(new DefaultMongoConnectionPoolTagsProvider().connectionPoolTags(event))
                                .and(Tag.of("client.name", clientName));
                    }
                });
    }
}
