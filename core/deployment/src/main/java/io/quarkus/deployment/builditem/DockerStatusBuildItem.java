package io.quarkus.deployment.builditem;

import io.quarkus.deployment.IsDockerWorking;

/**
 * A build item indicating the availability and status of the Docker runtime.
 * <p>
 * This extends {@link ContainerRuntimeStatusBuildItem} specifically for Docker.
 */
public final class DockerStatusBuildItem extends ContainerRuntimeStatusBuildItem {
    public DockerStatusBuildItem(IsDockerWorking isDockerWorking) {
        super(isDockerWorking);
    }

    /**
     * @deprecated Use {@link #isContainerRuntimeAvailable()} instead
     */
    @Deprecated(forRemoval = true)
    public boolean isDockerAvailable() {
        return isContainerRuntimeAvailable();
    }
}
