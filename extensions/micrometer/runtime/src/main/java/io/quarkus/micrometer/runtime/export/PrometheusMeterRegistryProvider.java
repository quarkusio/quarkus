package io.quarkus.micrometer.runtime.export;

import java.util.Map;

import javax.enterprise.inject.Produces;
import javax.inject.Singleton;
import javax.interceptor.Interceptor;

import org.jboss.logging.Logger;

import io.micrometer.core.instrument.Clock;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusDurationNamingConvention;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.micrometer.prometheus.PrometheusNamingConvention;
import io.prometheus.client.CollectorRegistry;
import io.quarkus.arc.AlternativePriority;
import io.quarkus.arc.DefaultBean;
import io.quarkus.arc.properties.UnlessBuildProperty;
import io.quarkus.micrometer.runtime.config.runtime.PrometheusRuntimeConfig;

@Singleton
public class PrometheusMeterRegistryProvider {
    private static final Logger log = Logger.getLogger(PrometheusMeterRegistryProvider.class);
    static final String PREFIX = "prometheus.";

    @Produces
    @Singleton
    @DefaultBean
    public PrometheusConfig configure(PrometheusRuntimeConfig config) {
        final Map<String, String> properties = ConfigAdapter.captureProperties(config.prometheus, PREFIX);
        return ConfigAdapter.validate(properties::get);
    }

    @Produces
    @DefaultBean
    public PrometheusNamingConvention namingConvention() {
        return new PrometheusNamingConvention();
    }

    @Produces
    @DefaultBean
    public PrometheusDurationNamingConvention durationNamingConvention() {
        return new PrometheusDurationNamingConvention();
    }

    @Produces
    @DefaultBean
    public CollectorRegistry collectorRegistry() {
        return new CollectorRegistry(true);
    }

    @Produces
    @Singleton
    @UnlessBuildProperty(name = "quarkus.micrometer.export.prometheus.default-registry", stringValue = "false", enableIfMissing = true)
    @AlternativePriority(Interceptor.Priority.APPLICATION + 100)
    public PrometheusMeterRegistry registry(PrometheusConfig config, CollectorRegistry collectorRegistry, Clock clock) {
        return new PrometheusMeterRegistry(config, collectorRegistry, clock);
    }
}
