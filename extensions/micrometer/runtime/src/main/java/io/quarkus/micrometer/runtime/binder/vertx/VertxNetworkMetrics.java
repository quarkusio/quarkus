package io.quarkus.micrometer.runtime.binder.vertx;

import java.util.Map;

import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.vertx.core.net.SocketAddress;
import io.vertx.core.spi.metrics.NetworkMetrics;

/**
 * NetworkMetrics<S>
 * <ul>
 * <li>S for Socket metric -- Vert.x Context</li>
 * </ul>
 */
public class VertxNetworkMetrics implements NetworkMetrics<Map<String, Object>> {
    final MeterRegistry registry;

    final String nameBytesRead;
    final String nameBytesWritten;
    final String nameExceptionOccurred;

    VertxNetworkMetrics(MeterRegistry registry, String prefix) {
        this.registry = registry;
        nameBytesRead = prefix + ".bytes.read";
        nameBytesWritten = prefix + ".bytes.written";
        nameExceptionOccurred = prefix + ".errors";
    }

    /**
     * Called when bytes have been read
     *
     * @param socketMetric the socket metric, null for UDP
     * @param remoteAddress the remote address which this socket received bytes from
     * @param numberOfBytes the number of bytes read
     */
    @Override
    public void bytesRead(Map<String, Object> socketMetric, SocketAddress remoteAddress, long numberOfBytes) {
        DistributionSummary.builder(nameBytesRead).register(registry).record(numberOfBytes);
    }

    /**
     * Called when bytes have been written
     *
     * @param socketMetric the socket metric, null for UDP
     * @param remoteAddress the remote address which bytes are being written to
     * @param numberOfBytes the number of bytes written
     */
    @Override
    public void bytesWritten(Map<String, Object> socketMetric, SocketAddress remoteAddress, long numberOfBytes) {
        DistributionSummary.builder(nameBytesWritten).register(registry).record(numberOfBytes);
    }

    /**
     * Called when exceptions occur for a specific connection.
     *
     * @param socketMetric the socket metric, null for UDP
     * @param remoteAddress the remote address of the connection or null if it's
     *        datagram/udp
     * @param t the exception that occurred
     */
    @Override
    public void exceptionOccurred(Map<String, Object> socketMetric, SocketAddress remoteAddress, Throwable t) {
        registry.counter(nameExceptionOccurred, "class", t.getClass().getName()).increment();
    }
}
