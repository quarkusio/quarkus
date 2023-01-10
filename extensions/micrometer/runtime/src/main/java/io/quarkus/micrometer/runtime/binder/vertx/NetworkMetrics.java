package io.quarkus.micrometer.runtime.binder.vertx;

import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.LongTaskTimer;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.vertx.core.net.SocketAddress;
import io.vertx.core.spi.metrics.TCPMetrics;

public class NetworkMetrics implements TCPMetrics<LongTaskTimer.Sample> {

    final MeterRegistry registry;
    final DistributionSummary received;
    final DistributionSummary sent;
    final String exception;

    final Tags tags;
    private final LongTaskTimer connDuration;

    public NetworkMetrics(MeterRegistry registry, Tags tags, String prefix, String receivedDesc, String sentDesc,
            String connDurationDesc) {
        this.registry = registry;
        this.tags = tags == null ? Tags.empty() : tags;
        received = DistributionSummary.builder(prefix + ".bytes.read")
                .description(receivedDesc)
                .tags(this.tags)
                .register(registry);
        sent = DistributionSummary.builder(prefix + ".bytes.written")
                .description(sentDesc)
                .tags(this.tags)
                .register(registry);
        connDuration = LongTaskTimer.builder(prefix + ".connections")
                .description(connDurationDesc)
                .tags(this.tags)
                .register(registry);
        // The exception has dynamic tags, so cannot be cached.
        exception = prefix + ".errors";
    }

    /**
     * Called when a client has connected, which is applicable for TCP connections.
     * <p>
     * The remote name of the client is a best effort to provide the name of the
     * remote host, i.e. if the name is specified at creation time, this name will be
     * used otherwise it will be the remote address.
     *
     * @param remoteAddress the remote address of the client
     * @param remoteName the remote name of the client
     * @return the sample
     */
    @Override
    public LongTaskTimer.Sample connected(SocketAddress remoteAddress, String remoteName) {
        return connDuration.start();
    }

    /**
     * Called when a client has disconnected, which is applicable for TCP
     * connections.
     *
     * @param sample the sample
     * @param remoteAddress the remote address of the client
     */
    @Override
    public void disconnected(LongTaskTimer.Sample sample, SocketAddress remoteAddress) {
        if (sample == null) {
            return;
        }
        sample.stop();
    }

    /**
     * Called when bytes have been read
     *
     * @param sample the sample, null for UDP
     * @param remoteAddress the remote address which this socket received bytes from
     * @param numberOfBytes the number of bytes read
     */
    @Override
    public void bytesRead(LongTaskTimer.Sample sample, SocketAddress remoteAddress, long numberOfBytes) {
        received.record(numberOfBytes);
    }

    /**
     * Called when bytes have been written
     *
     * @param sample the sample
     * @param remoteAddress the remote address which bytes are being written to
     * @param numberOfBytes the number of bytes written
     */
    @Override
    public void bytesWritten(LongTaskTimer.Sample sample, SocketAddress remoteAddress, long numberOfBytes) {
        sent.record(numberOfBytes);
    }

    /**
     * Called when exceptions occur for a specific connection.
     *
     * @param sample the sample
     * @param remoteAddress the remote address of the connection or null if it's
     *        datagram/udp
     * @param t the exception that occurred
     */
    @Override
    public void exceptionOccurred(LongTaskTimer.Sample sample, SocketAddress remoteAddress, Throwable t) {
        Tags copy = this.tags.and(Tag.of("class", t.getClass().getName()));
        registry.counter(exception, copy).increment();
    }

    public static String toString(SocketAddress remoteAddress) {
        if (remoteAddress.isDomainSocket()) {
            return "unix://" + remoteAddress.path();
        }
        String h = remoteAddress.hostName() == null ? remoteAddress.host() : remoteAddress.hostName();
        if (remoteAddress.port() > 0) {
            return h + ":" + remoteAddress.port();
        } else {
            return h;
        }
    }
}
