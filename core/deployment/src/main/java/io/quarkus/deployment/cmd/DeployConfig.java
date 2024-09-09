package io.quarkus.deployment.cmd;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * Deployment
 */
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public class DeployConfig {
    /**
     * Deployment target
     */
    @ConfigItem
    public Optional<String> target;

    public boolean isEnabled(String t) {
        return target.isEmpty() || target.get().equals(t);
    }
}
