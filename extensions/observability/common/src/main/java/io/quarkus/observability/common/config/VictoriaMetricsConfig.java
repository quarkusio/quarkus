package io.quarkus.observability.common.config;

import java.util.Optional;
import java.util.Set;

import io.quarkus.observability.common.ContainerConstants;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;

@ConfigGroup
@DevTarget("io.quarkus.observability.devresource.victoriametrics.VictoriaMetricsResource")
public interface VictoriaMetricsConfig extends ContainerConfig {
    @WithDefault(ContainerConstants.VICTORIA_METRICS)
    String imageName();

    @WithDefault("victoria-metrics")
    Optional<Set<String>> networkAliases();

    @WithDefault("8428")
    int port();

    @WithDefault("quarkus-dev-service-victoria-metrics")
    String label();
}
