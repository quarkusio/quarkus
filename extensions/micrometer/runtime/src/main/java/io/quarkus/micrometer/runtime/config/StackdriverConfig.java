package io.quarkus.micrometer.runtime.config;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class StackdriverConfig implements MicrometerConfig.CapabilityEnabled {
    /**
     * Support for export to Stackdriver.
     *
     * Support for Stackdriver will be enabled if Micrometer
     * support is enabled, the StackdriverMeterRegistry is on the classpath
     * and either this value is true, or this value is unset and
     * `quarkus.micrometer.registry-enabled-default` is true.
     *
     * [NOTE]
     * ====
     * Stackdriver libraries do not yet support running in native mode.
     * The Stackdriver MeterRegistry will be automatically disabled
     * for native builds.
     *
     * See https://github.com/grpc/grpc-java/issues/5460
     * ====
     *
     * @asciidoclet
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
