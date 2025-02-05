package io.quarkus.micrometer.runtime.config.runtime;

import java.util.Map;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithParentName;

/**
 * Runtime configuration for Micrometer meter registries.
 */
@SuppressWarnings("unused")
@ConfigMapping(prefix = "quarkus.micrometer.export.prometheus")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface PrometheusRuntimeConfig {
    // @formatter:off
    /**
     * Prometheus registry configuration properties.
     *
     * A property source for configuration of the Prometheus MeterRegistry,
     * see https://micrometer.io/docs/registry/prometheus.
     *
     * @asciidoclet
     */
    // @formatter:on
    @WithParentName
    @ConfigDocMapKey("configuration-property-name")
    Map<String, String> prometheus();
}
