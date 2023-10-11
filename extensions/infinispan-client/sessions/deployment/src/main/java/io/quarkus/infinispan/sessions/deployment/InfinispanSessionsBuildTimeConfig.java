package io.quarkus.infinispan.sessions.deployment;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * Configuration of Vert.x Web sessions stored in remote Infinispan cache.
 */
@ConfigRoot(name = "http.sessions.infinispan", phase = ConfigPhase.BUILD_TIME)
public class InfinispanSessionsBuildTimeConfig {
    /**
     * Name of the Infinispan client configured in the Quarkus Infinispan Client extension configuration.
     * If not set, uses the default (unnamed) Infinispan client.
     * <p>
     * Note that the Infinispan client must be configured to connect as a user with the necessary permissions
     * on the Infinispan server. The required minimum is equivalent to the Infinispan {@code deployer} role.
     */
    @ConfigItem
    public Optional<String> clientName;
}
