package io.quarkus.observability.common.config;

import java.util.Optional;
import java.util.Set;

import io.quarkus.observability.common.ContainerConstants;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;

@ConfigGroup
@DevTarget("io.quarkus.observability.devresource.jaeger.JaegerResource")
public interface JaegerConfig extends ContainerConfig {
    @WithDefault(ContainerConstants.JAEGER)
    String imageName();

    @WithDefault("jaeger")
    Optional<Set<String>> networkAliases();

    @WithDefault("quarkus-dev-service-jaeger")
    String label();
}
