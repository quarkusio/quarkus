package io.quarkus.micrometer.runtime.config;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class JmxConfig implements MicrometerConfig.CapabilityEnabled {
    /**
     * Support for export to JMX
     * <p>
     * Support for JMX will be enabled if Micrometer
     * support is enabled, the JmxMeterRegistry is on the classpath
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
