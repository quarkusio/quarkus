package io.quarkus.deployment.cmd;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;

/**
 * Deployment
 */
@ConfigMapping(prefix = "quarkus.deploy")
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public interface DeployConfig {
    /**
     * Deployment target
     */
    Optional<String> target();

    default boolean isEnabled(String t) {
        return target().isEmpty() || target().get().equals(t);
    }
}
