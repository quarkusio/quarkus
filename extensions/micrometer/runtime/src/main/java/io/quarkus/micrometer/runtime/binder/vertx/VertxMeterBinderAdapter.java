package io.quarkus.micrometer.runtime.binder.vertx;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.jboss.logging.Logger;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.quarkus.micrometer.runtime.binder.HttpBinderConfiguration;
import io.quarkus.vertx.http.runtime.ExtendedQuarkusVertxHttpMetrics;
import io.vertx.core.VertxOptions;
import io.vertx.core.datagram.DatagramSocketOptions;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.impl.NoStackTraceException;
import io.vertx.core.metrics.MetricsOptions;
import io.vertx.core.net.NetClientOptions;
import io.vertx.core.net.NetServerOptions;
import io.vertx.core.net.SocketAddress;
import io.vertx.core.spi.VertxMetricsFactory;
import io.vertx.core.spi.metrics.ClientMetrics;
import io.vertx.core.spi.metrics.DatagramSocketMetrics;
import io.vertx.core.spi.metrics.EventBusMetrics;
import io.vertx.core.spi.metrics.HttpClientMetrics;
import io.vertx.core.spi.metrics.HttpServerMetrics;
import io.vertx.core.spi.metrics.PoolMetrics;
import io.vertx.core.spi.metrics.TCPMetrics;
import io.vertx.core.spi.metrics.VertxMetrics;

public class VertxMeterBinderAdapter extends MetricsOptions
        implements VertxMetricsFactory, VertxMetrics, ExtendedQuarkusVertxHttpMetrics {
    private static final Logger log = Logger.getLogger(VertxMeterBinderAdapter.class);
    public static final String METRIC_NAME_SEPARATOR = "|";

    private HttpBinderConfiguration httpBinderConfiguration;

    public VertxMeterBinderAdapter() {
    }

    void setHttpConfig(HttpBinderConfiguration httpBinderConfiguration) {
        this.httpBinderConfiguration = httpBinderConfiguration;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public VertxMetricsFactory getFactory() {
        return this;
    }

    @Override
    public VertxMetrics metrics(VertxOptions vertxOptions) {
        return this;
    }

    @Override
    public MetricsOptions newOptions() {
        return this;
    }

    @Override
    public HttpServerMetrics<?, ?, ?> createHttpServerMetrics(HttpServerOptions options, SocketAddress localAddress) {
        if (httpBinderConfiguration == null) {
            throw new NoStackTraceException("HttpBinderConfiguration was not found");
        }
        if (httpBinderConfiguration.isServerEnabled()) {
            log.debugf("Create HttpServerMetrics with options %s and address %s", options, localAddress);
            return new VertxHttpServerMetrics(Metrics.globalRegistry, httpBinderConfiguration);
        }
        return null;
    }

    @Override
    public HttpClientMetrics<?, ?, ?, ?> createHttpClientMetrics(HttpClientOptions options) {
        if (httpBinderConfiguration == null) {
            return null;
        }
        if (httpBinderConfiguration.isClientEnabled()) {
            if (options.getMetricsName() == null || options.getMetricsName().trim().isEmpty()) {
                return null; // Not monitored, no name
            }

            // If the name is set, check if it follows the type/client-name syntax
            String clientName = extractClientName(options.getMetricsName());
            if (clientName != null) {
                return new VertxHttpClientMetrics(Metrics.globalRegistry, "http.client",
                        Tags.of(Tag.of("clientName", clientName)),
                        httpBinderConfiguration);
            } else {
                return new VertxHttpClientMetrics(Metrics.globalRegistry, "http.client",
                        Tags.of(Tag.of("clientName", "<default>")), httpBinderConfiguration);
            }
        }
        return null;
    }

    @Override
    public TCPMetrics<?> createNetServerMetrics(NetServerOptions options, SocketAddress localAddress) {
        return new VertxTcpServerMetrics(Metrics.globalRegistry, "tcp", Tags.of(
                Tag.of("port", Integer.toString(localAddress.port())),
                Tag.of("host", options.getHost()),
                Tag.of("address", VertxTcpServerMetrics.toString(localAddress))));
    }

    @Override
    public TCPMetrics<?> createNetClientMetrics(NetClientOptions options) {
        if (options.getMetricsName() == null || options.getMetricsName().trim().isEmpty()) {
            return null; // Not monitored, no name
        }
        // If the name is set, check if it follows the type/client-name syntax
        String prefix = extractPrefix(options.getMetricsName());
        String clientName = extractClientName(options.getMetricsName());
        if (clientName != null) {
            return new VertxTcpClientMetrics(Metrics.globalRegistry, prefix, Tags.of(Tag.of("clientName", clientName)));
        } else {
            return new VertxTcpClientMetrics(Metrics.globalRegistry, prefix, Tags.of(Tag.of("clientName", "<default>")));
        }
    }

    @Override
    public ClientMetrics<?, ?, ?, ?> createClientMetrics(SocketAddress remoteAddress, String type, String namespace) {
        // If the name is set, check if it follows the type/client-name syntax
        String prefix = extractPrefix(namespace);
        String clientName = extractClientName(namespace);
        if (clientName != null) {
            return new VertxClientMetrics(Metrics.globalRegistry, prefix, Tags.of(
                    Tag.of("clientName", clientName),
                    Tag.of("clientType", type)));
        } else {
            return new VertxClientMetrics(Metrics.globalRegistry, prefix, Tags.of(
                    Tags.of(Tag.of("clientName", "<default>"),
                            Tag.of("clientType", type))));
        }
    }

    @Override
    public PoolMetrics<?> createPoolMetrics(String poolType, String poolName, int maxPoolSize) {
        return new VertxPoolMetrics(Metrics.globalRegistry, poolType, poolName, maxPoolSize);
    }

    @Override
    public DatagramSocketMetrics createDatagramSocketMetrics(DatagramSocketOptions options) {
        return new VertxUdpMetrics(Metrics.globalRegistry, "udp", Tags.of("protocol", "udp"));
    }

    @Override
    public EventBusMetrics<?> createEventBusMetrics() {
        return new VertxEventBusMetrics(Metrics.globalRegistry, Tags.empty());
    }

    /**
     * Extract the prefix from the given metrics name.
     * This method applies a convention to be able to extract the prefix and the client name from the string returned by
     * {@link NetClientOptions#getMetricsName()}.
     * <p>
     * The convention is the following: {@code prefix|client name}. The choice of {@code |} has been done to avoid
     * separator commonly used in metrics name such as {@code _}, {@code .}, or {@code -}.
     *
     * @param mn the metric name
     * @return the prefix if the passed string follows the convention, the passed string if it does not.
     */
    private String extractPrefix(String mn) {
        if (mn.contains(METRIC_NAME_SEPARATOR)) {
            return mn.substring(0, mn.indexOf(METRIC_NAME_SEPARATOR));
        }
        return mn;
    }

    /**
     * Extract the client name from the given metrics name.
     * This method applies a convention to be able to extract the client name and the client name from the string
     * returned by {@link NetClientOptions#getMetricsName()}.
     * <p>
     * The convention is the following: {@code prefix|client name}. The choice of {@code |} has been done to avoid
     * separator commonly used in metrics name such as {@code _}, {@code .}, or {@code -}.
     *
     * @param mn the metric name
     * @return the client name if the passed string follows the convention, {@code null} otherwise.
     */
    private String extractClientName(String mn) {
        if (mn.contains(METRIC_NAME_SEPARATOR)) {
            return mn.substring(mn.indexOf(METRIC_NAME_SEPARATOR) + 1);
        }
        return null;
    }

    @Override
    public ConnectionTracker getHttpConnectionTracker() {
        return new ConnectionTracker() {

            private final Counter counter = Counter.builder("vertx.http.connections.rejected")
                    .description("Number of rejected HTTP connections")
                    .register(Metrics.globalRegistry);

            @Override
            public void onConnectionRejected() {
                counter.increment();
            }

            @Override
            public void initialize(int maxConnections, AtomicInteger current) {
                Gauge.builder("vertx.http.connections.current", new Supplier<Number>() {
                    @Override
                    public Number get() {
                        return current.get();
                    }
                })
                        .description("Current number of active HTTP connections")
                        .register(Metrics.globalRegistry);

                Gauge.builder("vertx.http.connections.max", new Supplier<Number>() {
                    @Override
                    public Number get() {
                        return maxConnections;
                    }
                })
                        .description("Max number of HTTP connections")
                        .register(Metrics.globalRegistry);
            }
        };
    }
}
