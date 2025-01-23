package io.quarkus.observability.common.config;

import java.time.Duration;

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
    @WithDefault("3000")
    int grafanaPort();

    /**
     * The timeout.
     */
    @WithDefault("PT3M")
    Duration timeout();
}
