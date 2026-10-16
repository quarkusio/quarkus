package io.quarkus.micrometer.runtime.export;

import java.util.Map;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusDurationNamingConvention;
import io.micrometer.prometheusmetrics.PrometheusNamingConvention;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import io.quarkus.arc.DefaultBean;
import io.quarkus.micrometer.runtime.config.runtime.PrometheusRuntimeConfig;

@Singleton
public class PrometheusMeterRegistryProvider {

    static final String PREFIX = "prometheus.";

    @Produces
    @Singleton
    @DefaultBean
    public PrometheusConfig configure(PrometheusRuntimeConfig config) {
        final Map<String, String> properties = ConfigAdapter.captureProperties(config.prometheus(), PREFIX);
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
    public PrometheusRegistry collectorRegistry() {
        return new PrometheusRegistry();
    }

}
