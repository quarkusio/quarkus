package io.quarkus.kubernetes.deployment;

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
     */
    @ConfigItem(defaultValue = "groundnuty/k8s-wait-for:no-root-v1.7")
    public String image;
}
