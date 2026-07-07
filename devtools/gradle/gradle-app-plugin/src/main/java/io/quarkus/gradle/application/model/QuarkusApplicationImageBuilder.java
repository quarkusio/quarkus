package io.quarkus.gradle.application.model;

/**
 * Container-image providers selectable for a named image operation.
 */
public enum QuarkusApplicationImageBuilder {
    /**
     * Jib.
     */
    JIB("jib"),
    /**
     * Docker.
     */
    DOCKER("docker"),
    /**
     * Podman.
     */
    PODMAN("podman"),
    /**
     * OpenShift binary builds.
     */
    OPENSHIFT("openshift"),
    /**
     * Cloud Native Buildpacks.
     */
    BUILDPACK("buildpack");

    private final String quarkusBuilderName;

    QuarkusApplicationImageBuilder(String quarkusBuilderName) {
        this.quarkusBuilderName = quarkusBuilderName;
    }

    /**
     * Returns the value used by Quarkus image-builder configuration.
     *
     * @return the Quarkus builder name
     */
    public String quarkusBuilderName() {
        return quarkusBuilderName;
    }
}
