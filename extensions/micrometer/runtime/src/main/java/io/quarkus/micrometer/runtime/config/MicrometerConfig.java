package io.quarkus.micrometer.runtime.config;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * Global configuration for the Micrometer extension
 */
@ConfigRoot(name = "micrometer", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public final class MicrometerConfig {

    /**
     * Micrometer metrics support.
     * <p>
     * Micrometer metrics support is enabled by default.
     */
    @ConfigItem(defaultValue = "true")
    public boolean enabled;

    /**
     * Micrometer MeterRegistry discovery.
     * <p>
     * Micrometer MeterRegistry implementations discovered on the classpath
     * will be enabled automatically by default.
     */
    @ConfigItem(defaultValue = "true")
    public boolean registryEnabledDefault;

    /**
     * Micrometer MeterBinder discovery.
     * <p>
     * Micrometer MeterBinder implementations discovered on the classpath
     * will be enabled automatically by default.
     */
    @ConfigItem(defaultValue = "true")
    public boolean binderEnabledDefault;

    /** Build / static runtime config for binders */
    public BinderConfig binder;

    /** Build / static runtime config for exporters */
    public ExportConfig export;

    /**
     * For MeterRegistry configurations with optional 'enabled' attributes,
     * determine whether or not the registry is enabled using {@link #registryEnabledDefault}
     * as the default value.
     */
    public boolean checkRegistryEnabledWithDefault(CapabilityEnabled config) {
        if (enabled) {
            Optional<Boolean> configValue = config.getEnabled();
            if (configValue.isPresent()) {
                return configValue.get();
            } else {
                return registryEnabledDefault;
            }
        }
        return false;
    }

    /**
     * For MeterBinder configurations with optional 'enabled' attributes,
     * determine whether or not the binder is enabled using {@link #binderEnabledDefault}
     * as the default value.
     */
    public boolean checkBinderEnabledWithDefault(CapabilityEnabled config) {
        if (enabled) {
            Optional<Boolean> configValue = config.getEnabled();
            if (configValue.isPresent()) {
                return configValue.get();
            } else {
                return binderEnabledDefault;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName()
                + "{enabled=" + enabled
                + ",binderEnabledDefault=" + binderEnabledDefault
                + ",registryEnabledDefault=" + registryEnabledDefault
                + '}';
    }

    /** Build / static runtime config for binders */
    @ConfigGroup
    public static class BinderConfig {
        public HttpClientConfigGroup httpClient;
        public HttpServerConfigGroup httpServer;

        /**
         * Micrometer JVM metrics support.
         * <p>
         * Support for JVM metrics will be enabled if Micrometer
         * support is enabled, and either this value is true, or this
         * value is unset and {@code quarkus.micrometer.binder-enabled-default} is true.
         */
        @ConfigItem
        public Optional<Boolean> jvm;

        public KafkaConfigGroup kafka;
        public MPMetricsConfigGroup mpMetrics;

        /**
         * Micrometer System metrics support.
         * <p>
         * Support for System metrics will be enabled if Micrometer
         * support is enabled, and either this value is true, or this
         * value is unset and {@code quarkus.micrometer.binder-enabled-default} is true.
         */
        @ConfigItem
        public Optional<Boolean> system;

        public VertxConfigGroup vertx;
    }

    /** Build / static runtime config for exporters */
    @ConfigGroup
    public static class ExportConfig {
        public JsonConfigGroup json;
        public PrometheusConfigGroup prometheus;
    }

    public static interface CapabilityEnabled {
        Optional<Boolean> getEnabled();
    }
}
