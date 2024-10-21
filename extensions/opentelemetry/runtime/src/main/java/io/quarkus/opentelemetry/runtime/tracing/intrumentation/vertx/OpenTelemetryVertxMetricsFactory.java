package io.quarkus.opentelemetry.runtime.tracing.intrumentation.vertx;

import java.util.Optional;

import io.quarkus.vertx.http.runtime.ExtendedQuarkusVertxHttpMetrics;
import io.vertx.core.Context;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.impl.HttpServerRequestInternal;
import io.vertx.core.net.SocketAddress;
import io.vertx.core.spi.VertxMetricsFactory;
import io.vertx.core.spi.metrics.HttpServerMetrics;
import io.vertx.core.spi.metrics.VertxMetrics;
import io.vertx.core.spi.observability.HttpRequest;

/**
 * This is used to retrieve the route name from Vert.x. This is useful for OpenTelemetry to generate the Span name and
 * <code>http.route</code> attribute. Right now, there is no other way to retrieve the route name from Vert.x using the
 * Telemetry SPI, so we need to rely on the Metrics SPI.
 * <p>
 * Right now, it is not possible to register multiple <code>VertxMetrics</code>, meaning that only a single one is
 * available per Quarkus instance. To avoid clashing with other extensions that provide Metrics data (like the
 * Micrometer extension), we only register the {@link OpenTelemetryVertxMetricsFactory} if the
 * <code>VertxHttpServerMetrics</code> is not available in the runtime.
 */
public class OpenTelemetryVertxMetricsFactory implements VertxMetricsFactory {
    @Override
    public VertxMetrics metrics(final VertxOptions options) {
        return new OpenTelemetryHttpServerMetrics();
    }

    public static class OpenTelemetryHttpServerMetrics
            implements HttpServerMetrics<OpenTelemetryHttpServerMetrics.MetricRequest, Object, Object>,
            VertxMetrics, ExtendedQuarkusVertxHttpMetrics {

        @Override
        public HttpServerMetrics<?, ?, ?> createHttpServerMetrics(final HttpServerOptions options,
                final SocketAddress localAddress) {
            return this;
        }

        @Override
        public MetricRequest requestBegin(final Object socketMetric, final HttpRequest request) {
            return MetricRequest.request(request);
        }

        @Override
        public void requestRouted(final MetricRequest requestMetric, final String route) {
            if (route != null) {
                requestMetric.getContext().ifPresent(context -> context.putLocal("VertxRoute", route));
            }
        }

        @Override
        public ConnectionTracker getHttpConnectionTracker() {
            // To be implemented if we decide to instrument with OpenTelemetry. See VertxMeterBinderAdapter for an example.
            return ExtendedQuarkusVertxHttpMetrics.NOOP_CONNECTION_TRACKER;
        }

        static final class MetricRequest {
            private final HttpRequest request;

            MetricRequest(final HttpRequest request) {
                this.request = request;
            }

            Optional<Context> getContext() {
                if (request instanceof HttpServerRequestInternal) {
                    return Optional.of(((HttpServerRequestInternal) request).context());
                } else {
                    return Optional.empty();
                }
            }

            static MetricRequest request(final HttpRequest httpRequest) {
                return new MetricRequest(httpRequest);
            }
        }
    }
}
