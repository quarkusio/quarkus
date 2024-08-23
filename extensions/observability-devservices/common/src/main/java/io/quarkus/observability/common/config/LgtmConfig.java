package io.quarkus.observability.common.config;

import java.util.Optional;
import java.util.Set;

import io.quarkus.observability.common.ContainerConstants;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;

@ConfigGroup
public interface LgtmConfig extends GrafanaConfig {

    /**
     * The name of the Grafana LGTM Docker image.
     */
    @WithDefault(ContainerConstants.LGTM)
    String imageName();

    /**
     * The Docker network aliases.
     */
    @WithDefault("lgtm,lgtm.testcontainer.docker")
    Optional<Set<String>> networkAliases();

    /**
     * The label of the container.
     */
    @WithDefault("quarkus-dev-service-lgtm")
    String label();

    /**
     * The port on which LGTM's OTLP port will be exposed.
     */
    @WithDefault("4318")
    int otlpPort();
}
