package io.quarkus.mongodb.metrics;

import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.Metric;
import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricType;
import org.eclipse.microprofile.metrics.Tag;

import com.mongodb.connection.ServerId;
import com.mongodb.event.ConnectionAddedEvent;
import com.mongodb.event.ConnectionCheckedInEvent;
import com.mongodb.event.ConnectionCheckedOutEvent;
import com.mongodb.event.ConnectionPoolClosedEvent;
import com.mongodb.event.ConnectionPoolListener;
import com.mongodb.event.ConnectionPoolOpenedEvent;
import com.mongodb.event.ConnectionRemovedEvent;

import io.smallrye.metrics.MetricRegistries;

public class MongoMetricsConnectionPoolListener implements ConnectionPoolListener {
    private final static String SIZE_NAME = "mongodb.connection-pool.size";
    private final static String CHECKED_OUT_COUNT_NAME = "mongodb.connection-pool.checked-out-count";

    @Override
    public void connectionPoolOpened(ConnectionPoolOpenedEvent event) {
        Tag[] tags = createTags(event.getServerId());

        registerGauge(SIZE_NAME, "the current size of the pool, including idle and and in-use members", tags);
        registerGauge(CHECKED_OUT_COUNT_NAME, "the current count of connections that are currently in use", tags);
    }

    @Override
    public void connectionPoolClosed(ConnectionPoolClosedEvent event) {
    }

    @Override
    public void connectionCheckedOut(ConnectionCheckedOutEvent event) {
        MetricID metricID = createMetricID(CHECKED_OUT_COUNT_NAME, event.getConnectionId().getServerId());

        Metric metric = getMetricRegistry().getMetrics().get(metricID);

        if (metric != null) {
            ((ConnectionPoolGauge) metric).increment();
        }
    }

    @Override
    public void connectionCheckedIn(ConnectionCheckedInEvent event) {
        MetricID metricID = createMetricID(CHECKED_OUT_COUNT_NAME, event.getConnectionId().getServerId());

        Metric metric = getMetricRegistry().getMetrics().get(metricID);

        if (metric != null) {
            ((ConnectionPoolGauge) metric).decrement();
        }
    }

    @Override
    public void connectionAdded(ConnectionAddedEvent event) {

        MetricID metricID = createMetricID(SIZE_NAME, event.getConnectionId().getServerId());

        Metric metric = getMetricRegistry().getMetrics().get(metricID);

        if (metric != null) {
            ((ConnectionPoolGauge) metric).increment();
        }
    }

    @Override
    public void connectionRemoved(ConnectionRemovedEvent event) {

        MetricID metricID = createMetricID(SIZE_NAME, event.getConnectionId().getServerId());

        Metric metric = getMetricRegistry().getMetrics().get(metricID);

        if (metric != null) {
            ((ConnectionPoolGauge) metric).decrement();
        }
    }

    private void registerGauge(String metricName, String description, Tag[] tags) {
        getMetricRegistry().remove(new MetricID(metricName, tags));

        Metadata metaData = Metadata.builder().withName(metricName).withType(MetricType.GAUGE)
                .withDescription(description).build();
        getMetricRegistry().register(metaData, new ConnectionPoolGauge(), tags);
    }

    private MetricRegistry getMetricRegistry() {
        return MetricRegistries.get(MetricRegistry.Type.VENDOR);
    }

    private Tag[] createTags(ServerId server) {
        return new Tag[] {
                new Tag("host", server.getAddress().getHost()),
                new Tag("port", String.valueOf(server.getAddress().getPort())),
        };
    }

    private MetricID createMetricID(String metricName, ServerId server) {
        return new MetricID(metricName, createTags(server));
    }
}
