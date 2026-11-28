package io.quarkus.dockerfiles.spi;

/**
 * Represents the different kinds of Dockerfiles that can be generated.
 */
public enum DockerfileKind {
    /**
     * Dockerfile for JVM-based containers
     */
    JVM,

    /**
     * Dockerfile for native containers
     */
    NATIVE
}