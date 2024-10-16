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

    // this is duplicated for a reason - not all collectors speak grpc,
    // which is the default in OTEL exporter,
    // where we want http as a default with LGTM

    /**
     * The LGTM's OTLP protocol.
     */
    @WithDefault(ContainerConstants.OTEL_HTTP_PROTOCOL)
    String otlpProtocol();
}
