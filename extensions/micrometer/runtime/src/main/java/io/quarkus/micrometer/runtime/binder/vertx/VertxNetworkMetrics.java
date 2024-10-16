package io.quarkus.micrometer.runtime.binder.vertx;

import java.util.Map;
import java.util.Objects;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
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
    final DistributionSummary nameBytesRead;
    final DistributionSummary nameBytesWritten;

    final Tags tags;

    private final Meter.MeterProvider<Counter> exceptions;

    VertxNetworkMetrics(MeterRegistry registry, String prefix, Tags tags) {
        this.registry = registry;
        this.tags = tags;
        DistributionSummary.Builder nameBytesReadBuilder = DistributionSummary.builder(prefix + ".bytes.read");
        DistributionSummary.Builder nameBytesWrittenBuilder = DistributionSummary.builder(prefix + ".bytes.written");
        if (tags != null) {
            nameBytesReadBuilder.tags(this.tags);
            nameBytesWrittenBuilder.tags(this.tags);
        }
        nameBytesRead = nameBytesReadBuilder.register(registry);
        nameBytesWritten = nameBytesWrittenBuilder.register(registry);

        exceptions = Counter.builder(prefix + ".errors")
                .description("Number of exceptions")
                .withRegistry(registry);
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
        nameBytesRead.record(numberOfBytes);
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
        nameBytesWritten.record(numberOfBytes);
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
        exceptions
                .withTags(Objects.requireNonNullElseGet(tags, Tags::empty).and(Tag.of("class", t.getClass().getName())))
                .increment();
    }

    @Override
    public void close() {
        nameBytesRead.close();
        nameBytesWritten.close();
    }
}
