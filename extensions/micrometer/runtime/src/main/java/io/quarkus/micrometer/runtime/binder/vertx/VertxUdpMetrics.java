package io.quarkus.micrometer.runtime.binder.vertx;

import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.vertx.core.net.SocketAddress;
import io.vertx.core.spi.metrics.DatagramSocketMetrics;

public class VertxUdpMetrics implements DatagramSocketMetrics {

    private final MeterRegistry registry;
    private volatile Tags tags;
    private final String exception;
    private final String read;
    private final String sent;

    public VertxUdpMetrics(MeterRegistry registry, String prefix, Tags tags) {
        this.registry = registry;
        this.tags = tags;

        sent = prefix + ".bytes.written";
        read = prefix + ".bytes.read";
        exception = prefix + ".errors";
    }

    @Override
    public void listening(String localName, SocketAddress localAddress) {
        tags = tags.and("address", NetworkMetrics.toString(localAddress));
    }

    @Override
    public void bytesRead(Void socketMetric, SocketAddress remoteAddress, long numberOfBytes) {
        DistributionSummary.builder(read)
                .description("Number of bytes read")
                .tags(tags.and("remote-address", NetworkMetrics.toString(remoteAddress)))
                .register(registry)
                .record(numberOfBytes);
    }

    @Override
    public void bytesWritten(Void socketMetric, SocketAddress remoteAddress, long numberOfBytes) {
        DistributionSummary.builder(sent)
                .description("Number of bytes written")
                .tags(tags.and("remote-address", NetworkMetrics.toString(remoteAddress)))
                .register(registry);
    }

    @Override
    public void exceptionOccurred(Void socketMetric, SocketAddress remoteAddress, Throwable t) {
        Tags copy = this.tags.and(Tag.of("class", t.getClass().getName()));
        registry.counter(exception, copy).increment();
    }
}
