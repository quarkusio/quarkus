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
     * <p>
     * Micrometer MeterBinder implementations discovered on the classpath
     * will be enabled automatically by default.
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
     */
    default boolean checkBinderEnabledWithDefault(CapabilityEnabled config) {
        if (enabled()) {
            Optional<Boolean> configValue = config.enabled();
            return configValue.orElseGet(this::binderEnabledDefault);
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

        MPMetricsConfigGroup mpMetrics();

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
