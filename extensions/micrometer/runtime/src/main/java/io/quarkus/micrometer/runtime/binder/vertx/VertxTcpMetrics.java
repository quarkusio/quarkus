package io.quarkus.micrometer.runtime.binder.vertx;

import io.micrometer.core.instrument.LongTaskTimer;
import io.micrometer.core.instrument.MeterRegistry;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.core.net.SocketAddress;
import io.vertx.core.spi.metrics.TCPMetrics;

public class VertxTcpMetrics extends VertxNetworkMetrics
        implements TCPMetrics<MetricsContext> {
    private static final String CONNECTED_SOCKET_SAMPLE = "CONNECTED_SOCKET_SAMPLE";

    final String nameConnections;

    VertxTcpMetrics(MeterRegistry registry, String prefix) {
        super(registry, prefix);
        nameConnections = prefix + ".connections";
    }

    /**
     * Called when a client has connected, which is applicable for TCP connections.
     * <p>
     * The remote name of the client is a best effort to provide the name of the
     * remote host, i.e if the name is specified at creation time, this name will be
     * used otherwise it will be the remote address.
     *
     * @param remoteAddress the remote address of the client
     * @param remoteName the remote name of the client
     * @return the socket metric
     */
    @Override
    public MetricsContext connected(SocketAddress remoteAddress, String remoteName) {
        Context vertxContext = Vertx.currentContext();
        MetricsContext metricsContext = MetricsContext.addMetricsContext(vertxContext);

        metricsContext.put(CONNECTED_SOCKET_SAMPLE,
                LongTaskTimer.builder(nameConnections).register(registry).start());
        return metricsContext;
    }

    /**
     * Called when a client has disconnected, which is applicable for TCP
     * connections.
     *
     * @param socketMetric the socket metric
     * @param remoteAddress the remote address of the client
     */
    @Override
    public void disconnected(MetricsContext socketMetric, SocketAddress remoteAddress) {
        if (socketMetric != null) {
            LongTaskTimer.Sample sample = (LongTaskTimer.Sample) socketMetric.get(CONNECTED_SOCKET_SAMPLE);
            if (sample != null) {
                sample.stop();
            }
            socketMetric.removeMetricsContext();
        }
    }
}
