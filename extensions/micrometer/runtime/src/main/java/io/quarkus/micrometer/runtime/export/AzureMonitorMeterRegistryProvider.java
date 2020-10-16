package io.quarkus.micrometer.runtime.export;

import java.util.Map;

import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import org.eclipse.microprofile.config.Config;

import io.micrometer.azuremonitor.AzureMonitorConfig;
import io.micrometer.azuremonitor.AzureMonitorMeterRegistry;
import io.micrometer.azuremonitor.AzureMonitorNamingConvention;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.config.MeterRegistryConfigValidator;
import io.micrometer.core.instrument.config.validate.Validated;
import io.micrometer.core.instrument.step.StepRegistryConfig;
import io.quarkus.arc.DefaultBean;

@Singleton
public class AzureMonitorMeterRegistryProvider {
    static final String PREFIX = "quarkus.micrometer.export.azuremonitor.";

    @Produces
    @Singleton
    @DefaultBean
    public AzureMonitorConfig configure(Config config) {
        final Map<String, String> properties = ConfigAdapter.captureProperties(config, PREFIX);

        return ConfigAdapter.validate(new AzureMonitorConfig() {
            @Override
            public String get(String key) {
                return properties.get(key);
            }

            @Override
            // Bug in micrometer: instrumentationKey is required
            public Validated<?> validate() {
                return MeterRegistryConfigValidator.checkAll(this,
                        c -> StepRegistryConfig.validate(c),
                        MeterRegistryConfigValidator.checkRequired("instrumentationKey",
                                AzureMonitorConfig::instrumentationKey));
            }
        });
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
