package io.quarkus.kubernetes.deployment;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class InitTaskConfig {
    /**
     * If true, the init task will be generated. Otherwise, the init task resource generation will be skipped.
     */
    @ConfigItem(defaultValue = "true")
    public boolean enabled;

    /**
     * The init task image to use by the init-container.
     * Deprecated, use waitForContainer.image instead.
     */
    @Deprecated
    @ConfigItem
    public Optional<String> image;

    /**
     * The configuration of the `wait for` container.
     */
    @ConfigItem
    public InitTaskContainerConfig waitForContainer;
}
