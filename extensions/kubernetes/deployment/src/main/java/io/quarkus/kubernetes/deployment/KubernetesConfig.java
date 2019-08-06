package io.quarkus.kubernetes.deployment;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public class KubernetesConfig {

    /**
     * The group of the application.
     * This value will be use as:
     * * docker image repo
     * * labeling resources
     */
    @ConfigItem
    public String group;

    /**
     * Configuration that is relevant to docker images
     */
    @ConfigItem
    public DockerConfig docker;
}
