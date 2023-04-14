package io.quarkus.observability.common.config;

import java.util.Optional;
import java.util.Set;

import io.quarkus.observability.common.ContainerConstants;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;

@ConfigGroup
public interface OTelConfig extends ContainerConfig {
    @WithDefault(ContainerConstants.OTEL)
    String imageName();

    @WithDefault("otel-collector")
    Optional<Set<String>> networkAliases();

    @WithDefault("quarkus-dev-service-otel")
    String label();

    Optional<String> configFile();
}
