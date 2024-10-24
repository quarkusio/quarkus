package io.quarkus.deployment;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Platform
 * <p>
 * This is used currently only to suppress warnings about unknown properties
 * when the user supplies something like: -Dquarkus.platform.group-id=someGroup
 *
 * TODO refactor code to actually use these values
 */
@ConfigMapping(prefix = "quarkus.platform")
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public interface PlatformConfig {

    /**
     * groupId of the platform to use
     */
    @WithDefault("io.quarkus.platform")
    String groupId();

    /**
     * artifactId of the platform to use
     */
    @WithDefault("quarkus-bom")
    String artifactId();

    /**
     * version of the platform to use
     */
    @WithDefault("999-SNAPSHOT")
    String version();
}
