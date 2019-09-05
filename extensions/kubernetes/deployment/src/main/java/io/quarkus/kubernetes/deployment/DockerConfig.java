package io.quarkus.kubernetes.deployment;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class DockerConfig {

    /**
     * The docker registry to which the images will be pushed
     */
    @ConfigItem(defaultValue = "docker.io")
    public String registry;
}
