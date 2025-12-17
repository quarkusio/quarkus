package io.quarkus.micrometer.runtime.config;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Global configuration for the Micrometer extension
 */
@ConfigMapping(prefix = "quarkus.micrometer")
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public interface MicrometerConfig {

    /**
     * Micrometer metrics support.
     * <p>
     * Micrometer metrics support is enabled by default.
     */
    @WithDefault("true")
    boolean enabled();

    /**
     * Micrometer MeterRegistry discovery.
     * <p>
     * Micrometer MeterRegistry implementations discovered on the classpath
     * will be enabled automatically by default.
     */
    @WithDefault("true")
    boolean registryEnabledDefault();

    /**
     * Micrometer MeterBinder discovery.
     * In other words, enables the automatic metrics instrumentation.
     * <p>
     * Micrometer MeterBinder implementations discovered on the classpath
     * will be enabled automatically by default. In other words, automatic metrics instrumentation will be ON by default.
     * <p>
     * <code>quarkus.micrometer.binder.enable-all</code> overrides this property, meaning when this is set to
     * <code>false</code>, and <code>enable-all</code> is true, discovery of all MeterBinder will still happen.
     */
    @WithDefault("true")
    boolean binderEnabledDefault();

    /** Build / static runtime config for binders */
    BinderConfig binder();

    /** Build / static runtime config for exporters */
    ExportConfig export();

    /**
     * For MeterRegistry configurations with optional 'enabled' attributes,
     * determine whether the registry is enabled using {@link #registryEnabledDefault}
     * as the default value.
     */
    default boolean checkRegistryEnabledWithDefault(CapabilityEnabled config) {
        if (enabled()) {
            Optional<Boolean> configValue = config.enabled();
            return configValue.orElseGet(this::registryEnabledDefault);
        }
        return false;
    }

    /**
     * For MeterBinder configurations with optional 'enabled' attributes,
     * determine whether the binder is enabled using {@link #binderEnabledDefault}
     * as the default value.
     *
     * @deprecated use {@link #isEnabled(CapabilityEnabled)} instead.
     */
    @Deprecated
    default boolean checkBinderEnabledWithDefault(CapabilityEnabled config) {
        if (enabled()) {
            Optional<Boolean> configValue = config.enabled();
            return configValue.orElseGet(this::binderEnabledDefault);
        }
        return false;
    }

    /**
     * Determines if a capability is enabled based on the {@link MicrometerConfig} configurations and the following rules:
     * <p>
     * <ul>
     * <li>
     * The {@link MicrometerConfig#enabled()} has precedence over all configurations, it means that if
     * <code>quarkus.micrometer.enabled</code>
     * is set to <code>false</code>, all metrics are disabled.
     * </li>
     * <li>
     * If the <code>quarkus.micrometer.binder.enable-all</code> is set to <code>true</code>, independently if the
     * parameter <code>aBoolean</code> resolve to <code>true</code> or <code>false</code> the metric will be enabled.
     * </li>
     * <li>
     * If the <code>quarkus.micrometer.binder.enable-all</code> is set to <code>false</code>, the parameter
     * <code>aBoolean</code>
     * will be used to determine if the metric is enabled or not. If <code>aBoolean</code> is empty, the metric will be
     * disabled.
     * </li>
     * </ul>
     *
     * @param aBoolean the optional boolean value to check if the capability is enabled
     * @return <code>true</code> if the capability is enabled, <code>false</code> otherwise.
     */
    default boolean isEnabled(Optional<Boolean> aBoolean) {
        if (enabled()) {
            if (this.binder().enableAll()) {
                return true;
            } else {
                return aBoolean.orElse(false);
            }
        }

        return false;
    }

    /**
     * Determines if a capability is enabled based on the {@link MicrometerConfig} configurations and the following rules:
     * <p>
     * <ul>
     * <li>
     * The {@link MicrometerConfig#enabled()} has precedence over all configurations, it means that if
     * <code>quarkus.micrometer.enabled</code>
     * is set to <code>false</code>, all metrics are disabled.
     * </li>
     * <li>
     * If the <code>quarkus.micrometer.binder.enable-all</code> is set to <code>true</code>, independently if the
     * parameter <code>aBoolean</code> resolve to <code>true</code> or <code>false</code> the metric will be enabled.
     * </li>
     * <li>
     * If the <code>quarkus.micrometer.binder.enable-all</code> is set to <code>false</code>, the parameter <code>config</code>
     * will be used to determine if the metric is enabled or not. If <code>config.enabled()</code> is empty, the
     * {@link MicrometerConfig#binderEnabledDefault()} will be used to determine if the metric is enabled or not.
     * </li>
     * </ul>
     *
     * @param config the {@link CapabilityEnabled} to check if the capability is enabled
     * @return <code>true</code> if the capability is enabled, <code>false</code> otherwise.
     */
    default boolean isEnabled(CapabilityEnabled config) {
        if (enabled()) {
            if (this.binder().enableAll()) {
                return true;
            } else {
                Optional<Boolean> configValue = config.enabled();
                return configValue.orElseGet(this::binderEnabledDefault);
            }
        }
        return false;
    }

    /** Build / static runtime config for binders */
    @ConfigGroup
    interface BinderConfig {
        HttpClientConfigGroup httpClient();

        HttpServerConfigGroup httpServer();

        /**
         * Micrometer JVM metrics support.
         * <p>
         * Support for JVM metrics will be enabled if Micrometer
         * support is enabled, and either this value is true, or this
         * value is unset and {@code quarkus.micrometer.binder-enabled-default} is true.
         */
        Optional<Boolean> jvm();

        KafkaConfigGroup kafka();

        RedisConfigGroup redis();

        StorkConfigGroup stork();

        GrpcServerConfigGroup grpcServer();

        GrpcClientConfigGroup grpcClient();

        ReactiveMessagingConfigGroup messaging();

        VirtualThreadsConfigGroup virtualThreads();

        /**
         * Micrometer System metrics support.
         * <p>
         * Support for System metrics will be enabled if Micrometer
         * support is enabled, and either this value is true, or this
         * value is unset and {@code quarkus.micrometer.binder-enabled-default} is true.
         */
        Optional<Boolean> system();

        VertxConfigGroup vertx();

        NettyConfigGroup netty();

        /**
         * Enable all binders.
         * Activates all metrics regardless off their particular default.
         * <p>
         * This property has precedence over all {@link BinderConfig} binders. In other words,
         * if the <code>quarkus.micrometer.binder.jvm</code> is set to <code>false</code> and
         * <code>quarkus.micrometer.binder.enabled-all</code> is set to <code>true</code>, all JVM metrics will be enabled.
         * <p>
         * Also takes precedence over <code>quarkus.micrometer.binder-enabled-default</code>, if binder discover is disabled,
         * discovery of all metrics will still happen.
         */
        @WithDefault("false")
        boolean enableAll();

    }

    /** Build / static runtime config for exporters */
    @ConfigGroup
    interface ExportConfig {
        JsonConfigGroup json();

        PrometheusConfigGroup prometheus();
    }

    interface CapabilityEnabled {

        /**
         * Gets enable value
         *
         * @return {@link Optional<Boolean>}
         */
        Optional<Boolean> enabled();
    }
}
