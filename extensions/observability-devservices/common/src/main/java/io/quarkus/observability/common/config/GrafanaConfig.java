package io.quarkus.observability.common.config;

import java.time.Duration;
import java.util.OptionalInt;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;

@ConfigGroup
public interface GrafanaConfig extends ContainerConfig {

    /**
     * The username.
     */
    @WithDefault("admin")
    String username();

    /**
     * The password.
     */
    @WithDefault("admin")
    String password();

    /**
     * The port of the Grafana container.
     */
    OptionalInt grafanaPort();

    /**
     * The timeout.
     */
    @WithDefault("3M")
    Duration timeout();
}
