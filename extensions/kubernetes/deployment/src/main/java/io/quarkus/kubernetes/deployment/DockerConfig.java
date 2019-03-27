package io.quarkus.kubernetes.deployment;

import io.quarkus.runtime.annotations.ConfigGroup;

@ConfigGroup
public class DockerConfig {

    /**
     * The docker registry to which the images will be pushed
     */
    public String registry = "docker.io";
}
