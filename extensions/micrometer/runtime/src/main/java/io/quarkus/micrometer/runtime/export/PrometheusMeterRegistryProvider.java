package io.quarkus.micrometer.runtime.export;

import java.util.Map;

import javax.enterprise.inject.Produces;
import javax.inject.Singleton;
import javax.interceptor.Interceptor;

import org.eclipse.microprofile.config.Config;
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

@Singleton
public class PrometheusMeterRegistryProvider {
    private static final Logger log = Logger.getLogger(PrometheusMeterRegistryProvider.class);

    static final String PREFIX = "quarkus.micrometer.export.prometheus.";

    @Produces
    @Singleton
    @DefaultBean
    public PrometheusConfig configure(Config config) {
        final Map<String, String> properties = ConfigAdapter.captureProperties(config, PREFIX);

        return ConfigAdapter.validate(new PrometheusConfig() {
            @Override
            public String get(String key) {
                return properties.get(key);
            }
        });
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
