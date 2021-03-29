package io.quarkus.micrometer.runtime.binder.vertx;

import java.util.HashMap;
import java.util.Map;

import io.micrometer.core.instrument.LongTaskTimer;
import io.micrometer.core.instrument.MeterRegistry;
import io.vertx.core.net.SocketAddress;
import io.vertx.core.spi.metrics.TCPMetrics;

public class VertxTcpMetrics extends VertxNetworkMetrics
        implements TCPMetrics<Map<String, Object>> {

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
     * @return a map for socket metric context
     */
    @Override
    public Map<String, Object> connected(SocketAddress remoteAddress, String remoteName) {
        Map<String, Object> socketMetric = new HashMap<>();
        socketMetric.put(CONNECTED_SOCKET_SAMPLE,
                LongTaskTimer.builder(nameConnections).register(registry).start());
        return socketMetric;
    }

    /**
     * Called when a client has disconnected, which is applicable for TCP
     * connections.
     *
     * @param socketMetric a Map for socket metric context or null
     * @param remoteAddress the remote address of the client
     */
    @Override
    public void disconnected(Map<String, Object> socketMetric, SocketAddress remoteAddress) {
        if (socketMetric == null) {
            return;
        }
        LongTaskTimer.Sample sample = (LongTaskTimer.Sample) socketMetric.get(CONNECTED_SOCKET_SAMPLE);
        if (sample != null) {
            sample.stop();
        }
    }
}
