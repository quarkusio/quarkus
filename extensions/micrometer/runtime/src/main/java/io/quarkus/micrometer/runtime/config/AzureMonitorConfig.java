package io.quarkus.micrometer.runtime.config;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class AzureMonitorConfig implements MicrometerConfig.CapabilityEnabled {
    /**
     * Support for export to AzureMonitor.
     * <p>
     * Support for AzureMonitor will be enabled if Micrometer
     * support is enabled, the AzureMonitorMeterRegistry is on the classpath
     * and either this value is true, or this value is unset and
     * {@code quarkus.micrometer.registry-enabled-default} is true.
     */
    @ConfigItem
    public Optional<Boolean> enabled;

    @Override
    public Optional<Boolean> getEnabled() {
        return enabled;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName()
                + "{enabled=" + enabled
                + '}';
    }
}
