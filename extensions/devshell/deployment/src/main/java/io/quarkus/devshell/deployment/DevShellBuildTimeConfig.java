package io.quarkus.devshell.deployment;

import java.util.List;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "quarkus.devshell")
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public interface DevShellBuildTimeConfig {

    /**
     * Whether Dev Shell is enabled in dev mode.
     */
    @WithDefault("true")
    boolean enabled();

    /**
     * Hosts allowed to connect to Dev Shell.
     * By default, only localhost connections are accepted.
     * Add additional hosts or IP addresses to this list to allow
     * remote connections (e.g., from a container or VM).
     */
    Optional<List<String>> allowedHosts();
}
