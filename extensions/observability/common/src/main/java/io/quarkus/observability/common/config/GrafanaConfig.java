package io.quarkus.observability.common.config;

import java.util.Optional;
import java.util.Set;

import io.quarkus.observability.common.ContainerConstants;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;

@ConfigGroup
public interface GrafanaConfig extends ContainerConfig {
    @WithDefault(ContainerConstants.GRAFANA)
    String imageName();

    @WithDefault("grafana,grafana.testcontainer.docker")
    Optional<Set<String>> networkAliases();

    @WithDefault("quarkus-dev-service-grafana")
    String label();

    @WithDefault("datasources.yaml")
    String datasourcesFile();
}
