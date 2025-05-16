package io.quarkus.deployment.builditem;

import io.quarkus.deployment.IsPodmanWorking;

/**
 * A build item indicating the availability and status of Podman.
 * <p>
 * This extends {@link ContainerRuntimeStatusBuildItem} specifically for Podman.
 * using {@link IsPodmanWorking} to check the status.
 */
public final class PodmanStatusBuildItem extends ContainerRuntimeStatusBuildItem {
    public PodmanStatusBuildItem(IsPodmanWorking isPodmanWorking) {
        super(isPodmanWorking);
    }
}
