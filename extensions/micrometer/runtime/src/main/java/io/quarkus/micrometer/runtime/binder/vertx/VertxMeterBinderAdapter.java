package io.quarkus.micrometer.runtime.binder.vertx;

import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Singleton;

import org.jboss.logging.Logger;

import io.micrometer.core.instrument.MeterRegistry;
import io.quarkus.micrometer.runtime.binder.HttpBinderConfiguration;
import io.quarkus.micrometer.runtime.config.runtime.VertxConfig;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.metrics.MetricsOptions;
import io.vertx.core.net.SocketAddress;
import io.vertx.core.spi.VertxMetricsFactory;
import io.vertx.core.spi.metrics.HttpServerMetrics;
import io.vertx.core.spi.metrics.VertxMetrics;

@Singleton
public class VertxMeterBinderAdapter extends MetricsOptions implements VertxMetricsFactory, VertxMetrics {
    private static final Logger log = Logger.getLogger(VertxMeterBinderAdapter.class);

    private final static AtomicReference<MeterRegistry> meterRegistryRef = new AtomicReference<>();

    private VertxConfig config;
    private HttpBinderConfiguration httpBinderConfiguration;

    public VertxMeterBinderAdapter() {
    }

    public void setVertxConfig(VertxConfig config, HttpBinderConfiguration httpBinderConfiguration) {
        this.config = config;
        this.httpBinderConfiguration = httpBinderConfiguration;
    }

    public static void setMeterRegistry(MeterRegistry meterRegistry) {
        meterRegistryRef.set(meterRegistry);
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
        log.debugf("Bind registry %s to Vertx Metrics", meterRegistryRef.get());
        MeterRegistry registry = meterRegistryRef.get();
        if (registry == null) {
            throw new IllegalStateException("MeterRegistry was not resolved");
        }
        if (config == null) {
            throw new IllegalStateException("VertxConfig was not found");
        }
        if (httpBinderConfiguration.isServerEnabled()) {
            log.debugf("Create HttpServerMetrics with options %s and address %s", options, localAddress);
            return new VertxHttpServerMetrics(registry, httpBinderConfiguration);
        }
        return null;
    }
}
