package io.quarkus.micrometer.runtime.export;

import java.util.Map;

import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import org.eclipse.microprofile.config.Config;

import io.micrometer.azuremonitor.AzureMonitorConfig;
import io.micrometer.azuremonitor.AzureMonitorMeterRegistry;
import io.micrometer.azuremonitor.AzureMonitorNamingConvention;
import io.micrometer.core.instrument.Clock;
import io.quarkus.arc.DefaultBean;
import io.quarkus.micrometer.runtime.MicrometerRecorder;

@Singleton
public class AzureMonitorMeterRegistryProvider {
    static final String PREFIX = "quarkus.micrometer.export.azuremonitor.";

    @Produces
    @Singleton
    @DefaultBean
    public AzureMonitorConfig configure(Config config) {
        final Map<String, String> properties = MicrometerRecorder.captureProperties(config, PREFIX);

        return new AzureMonitorConfig() {
            @Override
            public String get(String key) {
                return properties.get(key);
            }
        };
    }

    @Produces
    @DefaultBean
    public AzureMonitorNamingConvention namingConvention() {
        return new AzureMonitorNamingConvention();
    }

    @Produces
    @Singleton
    public AzureMonitorMeterRegistry registry(AzureMonitorConfig config, Clock clock) {
        return new AzureMonitorMeterRegistry(config, clock);
    }
}
