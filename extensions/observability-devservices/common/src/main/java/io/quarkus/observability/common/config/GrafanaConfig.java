package io.quarkus.observability.common.config;

import java.time.Duration;

import io.smallrye.config.WithDefault;

public interface GrafanaConfig extends ContainerConfig {

    // copied from ContainerConfig, config hierarchy workaround

    @WithDefault("true")
    boolean enabled();

    @WithDefault("true")
    boolean shared();

    @WithDefault("quarkus")
    String serviceName();

    // ---

    @WithDefault("admin")
    String username();

    @WithDefault("admin")
    String password();

    @WithDefault("3000")
    int grafanaPort();

    @WithDefault("PT1M")
    Duration timeout();
}
