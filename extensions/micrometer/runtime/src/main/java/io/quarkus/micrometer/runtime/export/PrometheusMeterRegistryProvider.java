package io.quarkus.micrometer.runtime.export;

import java.util.Map;

import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import org.eclipse.microprofile.config.Config;

import io.micrometer.core.instrument.Clock;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.prometheus.client.CollectorRegistry;
import io.quarkus.arc.DefaultBean;
import io.quarkus.micrometer.runtime.MicrometerRecorder;

@Singleton
public class PrometheusMeterRegistryProvider {
    static final String PREFIX = "quarkus.micrometer.export.prometheus.";

    @Produces
    @Singleton
    @DefaultBean
    public PrometheusConfig configure(Config config) {
        final Map<String, String> properties = MicrometerRecorder.captureProperties(config, PREFIX);

        return new PrometheusConfig() {
            @Override
            public String get(String key) {
                return properties.get(key);
            }
        };
    }

    @Produces
    @DefaultBean
    public CollectorRegistry collectorRegistry() {
        return new CollectorRegistry(true);
    }

    @Produces
    @Singleton
    // Quarkus #8895 : @AlternativePriority(Interceptor.Priority.APPLICATION + 100)
    public PrometheusMeterRegistry registry(PrometheusConfig config, CollectorRegistry collectorRegistry, Clock clock) {
        return new PrometheusMeterRegistry(config, collectorRegistry, clock);
    }
}
