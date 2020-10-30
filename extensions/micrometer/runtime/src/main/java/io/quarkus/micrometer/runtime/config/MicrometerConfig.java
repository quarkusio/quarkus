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
        /**
         * Micrometer JVM metrics support.
         * <p>
         * Micrometer JVM metrics support is enabled by default.
         */
        @ConfigItem(defaultValue = "true")
        public boolean jvm;

        public KafkaConfig kafka;
        public MicroprofileMetricsConfig mpMetrics;

        /**
         * Micrometer System metrics support.
         * <p>
         * Micrometer System metrics support is enabled by default.
         */
        @ConfigItem(defaultValue = "true")
        public boolean system;

        public VertxConfig vertx;
    }

    /** Build / static runtime config for exporters */
    @ConfigGroup
    public static class ExportConfig {
        public AzureMonitorConfig azuremonitor;
        public DatadogConfig datadog;
        public JmxConfig jmx;
        public JsonConfig json;
        public PrometheusConfig prometheus;
        public SignalFxConfig signalfx;
        public StackdriverConfig stackdriver;
        public StatsdConfig statsd;
    }

    public static interface CapabilityEnabled {
        Optional<Boolean> getEnabled();
    }
}
