package io.quarkus.micrometer.runtime.export;

import java.util.Map;

import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusDurationNamingConvention;
import io.micrometer.prometheus.PrometheusNamingConvention;
import io.prometheus.client.CollectorRegistry;
import io.quarkus.arc.DefaultBean;
import io.quarkus.micrometer.runtime.config.runtime.PrometheusRuntimeConfig;

@Singleton
public class PrometheusMeterRegistryProvider {

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

}
