package io.quarkus.deployment.builditem;

import io.quarkus.deployment.IsDockerWorking;

/**
 * A build item that represents the status of the Docker container runtime.
 * <p>
 * This class extends {@link ContainerRuntimeStatusBuildItem} and provides
 * a specific implementation for checking the availability of the Docker runtime.
 * </p>
 */
public final class DockerStatusBuildItem extends ContainerRuntimeStatusBuildItem {

    /**
     * Constructs a new {@link DockerStatusBuildItem}.
     *
     * @param isDockerWorking a functional interface to check if the Docker runtime is working
     */
    public DockerStatusBuildItem(IsDockerWorking isDockerWorking) {
        super(isDockerWorking);
    }

    /**
     * Checks if the Docker runtime is available.
     * <p>
     * This method is deprecated and will be removed in a future release.
     * Use {@link #isContainerRuntimeAvailable()} instead.
     * </p>
     *
     * @return {@code true} if the Docker runtime is available, {@code false} otherwise
     * @deprecated Use {@link #isContainerRuntimeAvailable()} instead
     */
    @Deprecated(forRemoval = true)
    public boolean isDockerAvailable() {
        return isContainerRuntimeAvailable();
    }
}