package io.quarkus.mongodb.metrics;

import com.mongodb.event.ConnectionPoolListener;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.binder.mongodb.MongoMetricsConnectionPoolListener;

public class MicrometerConnectionPoolListener {
    public static ConnectionPoolListener createMicrometerConnectionPool() {
        return new MongoMetricsConnectionPoolListener(Metrics.globalRegistry);
    }
}
