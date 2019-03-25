package io.quarkus.kubernetes.deployment;

import io.quarkus.runtime.annotations.ConfigGroup;

@ConfigGroup
public class DockerConfig {

    /**
     * The docker registry to which the images will be pushed
     */
    public String registry = "docker.io";

    /**
     * Determines whether a docker image will be built when the build ends
     */
    public boolean build = false;

    /**
     * Determines whether the built docker image will be pushed
     */
    public boolean push = false;
}
