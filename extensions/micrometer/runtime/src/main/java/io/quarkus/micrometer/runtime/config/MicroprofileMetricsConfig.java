package io.quarkus.micrometer.runtime.config;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

/**
 * Build / static runtime config for the Microprofile Metrics Binder
 */
@ConfigGroup
public class MicroprofileMetricsConfig implements MicrometerConfig.CapabilityEnabled {
    /**
     * Microprofile Metrics support.
     * <p>
     * Support for Microprofile metrics will be enabled if micrometer
     * support is enabled, and this value is true. You need to also
     * include the microprofile api jar to your dependencies:
     *
     * <pre>
     * &lt;dependency&gt;
     *   &lt;groupId&gt;org.eclipse.microprofile.metrics&lt;/groupId&gt;
     *   &lt;artifactId&gt;microprofile-metrics-api&lt;/artifactId&gt;
     * &lt;/dependency&gt;
     * </pre>
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
