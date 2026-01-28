package io.quarkus.aesh.ssh.deployment;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
@ConfigMapping(prefix = "quarkus.aesh.ssh")
public interface AeshSshBuildTimeConfig {

    /**
     * Whether the health check is published when the smallrye-health extension is present.
     */
    @WithName("health.enabled")
    @WithDefault("true")
    boolean healthEnabled();
}
