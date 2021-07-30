package io.quarkus.deployment;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * This is used currently only to suppress warnings about unknown properties
 * when the user supplies something like: -Dquarkus.platform.group-id=someGroup
 *
 * TODO refactor code to actually use these values
 */
@ConfigRoot
public class PlatformConfig {

    /**
     * groupId of the platform to use
     */
    @ConfigItem(defaultValue = "io.quarkus.platform")
    String groupId;

    /**
     * artifactId of the platform to use
     */
    @ConfigItem(defaultValue = "quarkus-bom")
    String artifactId;

    /**
     * version of the platform to use
     */
    @ConfigItem(defaultValue = "999-SNAPSHOT")
    String version;
}
