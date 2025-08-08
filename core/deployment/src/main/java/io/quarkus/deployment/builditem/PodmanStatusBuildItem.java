package io.quarkus.deployment.builditem;

import io.quarkus.deployment.IsPodmanWorking;

/**
 * A build item that represents the status of the Podman container runtime.
 * <p>
 * This class extends {@link ContainerRuntimeStatusBuildItem} and provides
 * a specific implementation for checking the availability of the Podman runtime.
 * </p>
 */
public final class PodmanStatusBuildItem extends ContainerRuntimeStatusBuildItem {

    /**
     * Constructs a new {@link PodmanStatusBuildItem}.
     *
     * @param isPodmanWorking a functional interface to check if the Podman runtime is working
     */
    public PodmanStatusBuildItem(IsPodmanWorking isPodmanWorking) {
        super(isPodmanWorking);
    }
}