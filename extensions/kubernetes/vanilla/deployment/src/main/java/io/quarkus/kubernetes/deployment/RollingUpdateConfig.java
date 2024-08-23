package io.quarkus.kubernetes.deployment;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class RollingUpdateConfig {
    /**
     * Specifies the maximum number of Pods that can be unavailable during the update process.
     */
    @ConfigItem(defaultValue = "25%")
    String maxUnavailable;

    /**
     * Specifies the maximum number of Pods that can be created over the desired number of Pods.
     */
    @ConfigItem(defaultValue = "25%")
    String maxSurge;
}
