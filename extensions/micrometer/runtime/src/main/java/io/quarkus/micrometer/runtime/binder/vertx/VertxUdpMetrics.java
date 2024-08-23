package io.quarkus.micrometer.runtime.binder.vertx;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.vertx.core.net.SocketAddress;
import io.vertx.core.spi.metrics.DatagramSocketMetrics;

public class VertxUdpMetrics implements DatagramSocketMetrics {

    private volatile Tags tags;

    private final Meter.MeterProvider<DistributionSummary> read;
    private final Meter.MeterProvider<DistributionSummary> sent;
    private final Meter.MeterProvider<Counter> exceptions;

    public VertxUdpMetrics(MeterRegistry registry, String prefix, Tags tags) {
        this.tags = tags;

        read = DistributionSummary.builder(prefix + ".bytes.read")
                .description("Number of bytes read")
                .withRegistry(registry);
        sent = DistributionSummary.builder(prefix + ".bytes.written")
                .description("Number of bytes written")
                .withRegistry(registry);

        exceptions = Counter.builder(prefix + ".errors")
                .description("Number of exceptions")
                .withRegistry(registry);
    }

    @Override
    public void listening(String localName, SocketAddress localAddress) {
        tags = tags.and("address", NetworkMetrics.toString(localAddress));
    }

    @Override
    public void bytesRead(Void socketMetric, SocketAddress remoteAddress, long numberOfBytes) {
        read.withTags(tags.and("remote-address", NetworkMetrics.toString(remoteAddress)))
                .record(numberOfBytes);
    }

    @Override
    public void bytesWritten(Void socketMetric, SocketAddress remoteAddress, long numberOfBytes) {
        sent.withTags(tags.and("remote-address", NetworkMetrics.toString(remoteAddress)))
                .record(numberOfBytes);
    }

    @Override
    public void exceptionOccurred(Void socketMetric, SocketAddress remoteAddress, Throwable t) {
        Tags copy = this.tags.and(Tag.of("class", t.getClass().getName()));
        exceptions.withTags(copy).increment();
    }
}
