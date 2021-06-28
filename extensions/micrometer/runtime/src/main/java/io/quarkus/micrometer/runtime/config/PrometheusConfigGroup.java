package io.quarkus.micrometer.runtime.config;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class PrometheusConfigGroup implements MicrometerConfig.CapabilityEnabled {
    /**
     * Support for export to Prometheus.
     * <p>
     * Support for Prometheus will be enabled if Micrometer
     * support is enabled, the PrometheusMeterRegistry is on the classpath
     * and either this value is true, or this value is unset and
     * {@code quarkus.micrometer.registry-enabled-default} is true.
     */
    @ConfigItem
    public Optional<Boolean> enabled;

    /**
     * The path for the prometheus metrics endpoint (produces text/plain).
     * The default value is {@code metrics}.
     */
    @ConfigItem(defaultValue = "metrics")
    public String path;

    /**
     * By default, this extension will create a Prometheus MeterRegistry instance.
     * <p>
     * Use this attribute to veto the creation of the default Prometheus MeterRegistry.
     */
    @ConfigItem(defaultValue = "true")
    public boolean defaultRegistry;

    @Override
    public Optional<Boolean> getEnabled() {
        return enabled;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName()
                + "{path='" + path
                + ",enabled=" + enabled
                + ",defaultRegistry=" + defaultRegistry
                + '}';
    }
}
