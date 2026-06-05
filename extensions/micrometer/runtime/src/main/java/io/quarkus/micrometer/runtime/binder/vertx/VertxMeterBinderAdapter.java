package io.quarkus.micrometer.runtime.binder.vertx;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Supplier;

import org.jboss.logging.Logger;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.quarkus.micrometer.runtime.binder.HttpBinderConfiguration;
import io.quarkus.micrometer.runtime.export.exemplars.OpenTelemetryContextUnwrapper;
import io.quarkus.micrometer.runtime.meters.Gauges;
import io.quarkus.vertx.http.runtime.ExtendedQuarkusVertxHttpMetrics;
import io.vertx.core.VertxException;
import io.vertx.core.VertxOptions;
import io.vertx.core.datagram.DatagramSocketOptions;
import io.vertx.core.http.HttpClientConfig;
import io.vertx.core.http.HttpServerConfig;
import io.vertx.core.metrics.MetricsOptions;
import io.vertx.core.net.*;
import io.vertx.core.spi.VertxMetricsFactory;
import io.vertx.core.spi.metrics.*;

public class VertxMeterBinderAdapter extends MetricsOptions
        implements VertxMetricsFactory, VertxMetrics, ExtendedQuarkusVertxHttpMetrics {
    private static final Logger log = Logger.getLogger(VertxMeterBinderAdapter.class);
    public static final String METRIC_NAME_SEPARATOR = "|";

    private final Gauges<LongAdder> longAdderGauges = Gauges.longAdder();
    private final Gauges<AtomicReference<Double>> doubleGauges = Gauges.of(() -> new AtomicReference<>(0.0));

    private HttpBinderConfiguration httpBinderConfiguration;
    private OpenTelemetryContextUnwrapper openTelemetryContextUnwrapper;

    public VertxMeterBinderAdapter() {
    }

    void initBinder(HttpBinderConfiguration httpBinderConfiguration,
            OpenTelemetryContextUnwrapper openTelemetryContextUnwrapper) {
        this.openTelemetryContextUnwrapper = openTelemetryContextUnwrapper;
        this.httpBinderConfiguration = httpBinderConfiguration;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

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
    public HttpServerMetrics<?, ?> createHttpServerMetrics(final HttpServerConfig options,
            final SocketAddress tcpLocalAddress, final SocketAddress localAddress) {
        if (httpBinderConfiguration == null) {
            throw new VertxException("HttpBinderConfiguration was not found");
        }
        if (openTelemetryContextUnwrapper == null) {
            throw new VertxException("OpenTelemetryContextUnwrapper was not found");
        }
        if (httpBinderConfiguration.isServerEnabled()) {
            log.debugf("Create HttpServerMetrics with options %s and address %s", options, localAddress);
            return new VertxHttpServerMetrics(Metrics.globalRegistry, httpBinderConfiguration, openTelemetryContextUnwrapper,
                    options, longAdderGauges);
        }
        return null;
    }

    @Override
    public HttpClientMetrics<?, ?> createHttpClientMetrics(HttpClientConfig config) {
        if (httpBinderConfiguration == null) {
            return null;
        }
        if (httpBinderConfiguration.isClientEnabled()) {
            String metricsName = config.getObservabilityConfig() != null
                    ? config.getObservabilityConfig().getMetricsName()
                    : null;
            if (metricsName == null || metricsName.trim().isEmpty()) {
                return null; // Not monitored, no name
            }

            String prefix = extractPrefix(metricsName);
            boolean isRestClient = "rest-client".equals(prefix);
            // If the name is set, check if it follows the type/client-name syntax
            String clientName = extractClientName(metricsName);
            if (clientName != null) {
                return new VertxHttpClientMetrics(Metrics.globalRegistry, "http.client",
                        Tags.of(Tag.of("clientName", clientName)),
                        httpBinderConfiguration, isRestClient, longAdderGauges);
            } else {
                return new VertxHttpClientMetrics(Metrics.globalRegistry, "http.client",
                        Tags.of(Tag.of("clientName", "<default>")), httpBinderConfiguration, isRestClient,
                        longAdderGauges);
            }
        }
        return null;
    }

    @Override
    public TransportMetrics<?> createTcpServerMetrics(TcpServerConfig config, String protocol, SocketAddress localAddress) {
        return new VertxTcpServerMetrics(Metrics.globalRegistry, "tcp", Tags.of(
                Tag.of("port", Integer.toString(localAddress.port())),
                Tag.of("protocol", protocol),
                Tag.of("host", config.getHost()),
                Tag.of("address", VertxTcpServerMetrics.toString(localAddress))), longAdderGauges);
    }

    @Override
    public TransportMetrics<?> createTcpClientMetrics(TcpClientConfig config, String protocol) {
        if (config.getMetricsName() == null || config.getMetricsName().trim().isEmpty()) {
            return null; // Not monitored, no name
        }
        // If the name is set, check if it follows the type/client-name syntax
        String prefix = extractPrefix(config.getMetricsName());
        String clientName = extractClientName(config.getMetricsName());
        if (clientName != null) {
            return new VertxTcpClientMetrics(Metrics.globalRegistry, prefix, Tags.of(Tag.of("clientName", clientName)),
                    longAdderGauges);
        } else {
            return new VertxTcpClientMetrics(Metrics.globalRegistry, prefix, Tags.of(Tag.of("clientName", "<default>")),
                    longAdderGauges);
        }
    }

    @Override
    public ClientMetrics<?, ?, ?> createClientMetrics(SocketAddress remoteAddress, String type, String namespace) {
        // If the name is set, check if it follows the type/client-name syntax
        String prefix = extractPrefix(namespace);
        String clientName = extractClientName(namespace);
        if (clientName != null) {
            return new VertxClientMetrics(Metrics.globalRegistry, prefix, Tags.of(
                    Tag.of("clientName", clientName),
                    Tag.of("clientType", type)), longAdderGauges);
        } else {
            return new VertxClientMetrics(Metrics.globalRegistry, prefix, Tags.of(
                    Tags.of(Tag.of("clientName", "<default>"),
                            Tag.of("clientType", type))),
                    longAdderGauges);
        }
    }

    @Override
    public PoolMetrics<?, ?> createPoolMetrics(String poolType, String poolName, int maxPoolSize) {
        return new VertxPoolMetrics(Metrics.globalRegistry, poolType, poolName, maxPoolSize,
                longAdderGauges, doubleGauges);
    }

    @Override
    public DatagramSocketMetrics createDatagramSocketMetrics(DatagramSocketOptions options) {
        return new VertxUdpMetrics(Metrics.globalRegistry, "udp", Tags.of("protocol", "udp"));
    }

    @Override
    public EventBusMetrics<?> createEventBusMetrics() {
        return new VertxEventBusMetrics(Metrics.globalRegistry, Tags.empty(), longAdderGauges);
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
    static String extractClientName(String mn) {
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
            private final Gauges<AtomicInteger> atomicIntGauges = Gauges.atomicInteger();

            @Override
            public void onConnectionRejected() {
                counter.increment();
            }

            @Override
            public void initialize(int maxConnections, AtomicInteger current) {
                atomicIntGauges.builder("vertx.http.connections.current", AtomicInteger::doubleValue)
                        .description("Current number of active HTTP connections")
                        .register(Metrics.globalRegistry, current);

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
