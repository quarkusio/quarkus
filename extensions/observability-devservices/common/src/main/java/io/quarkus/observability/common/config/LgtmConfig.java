package io.quarkus.observability.common.config;

import java.util.Optional;
import java.util.Set;

import io.quarkus.observability.common.ContainerConstants;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;

@ConfigGroup
public interface LgtmConfig extends GrafanaConfig {
    @WithDefault(ContainerConstants.LGTM)
    String imageName();

    @WithDefault("lgtm,lgtm.testcontainer.docker")
    Optional<Set<String>> networkAliases();

    @WithDefault("quarkus-dev-service-lgtm")
    String label();

    @WithDefault("4318")
    int otlpPort();
}
