package io.quarkus.micrometer.runtime.binder.vertx;

import org.jboss.logging.Logger;

import io.micrometer.core.instrument.Metrics;
import io.quarkus.micrometer.runtime.binder.HttpBinderConfiguration;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.metrics.MetricsOptions;
import io.vertx.core.net.SocketAddress;
import io.vertx.core.spi.VertxMetricsFactory;
import io.vertx.core.spi.metrics.HttpServerMetrics;
import io.vertx.core.spi.metrics.VertxMetrics;

public class VertxMeterBinderAdapter extends MetricsOptions implements VertxMetricsFactory, VertxMetrics {
    private static final Logger log = Logger.getLogger(VertxMeterBinderAdapter.class);

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
            throw new IllegalStateException("HttpBinderConfiguration was not found");
        }
        if (httpBinderConfiguration.isServerEnabled()) {
            log.debugf("Create HttpServerMetrics with options %s and address %s", options, localAddress);
            return new VertxHttpServerMetrics(Metrics.globalRegistry, httpBinderConfiguration);
        }
        return null;
    }
}
