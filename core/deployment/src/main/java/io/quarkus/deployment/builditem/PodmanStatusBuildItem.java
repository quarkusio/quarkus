package io.quarkus.deployment.builditem;

import io.quarkus.deployment.IsPodmanWorking;

public final class PodmanStatusBuildItem extends ContainerRuntimeStatusBuildItem {
    public PodmanStatusBuildItem(IsPodmanWorking isPodmanWorking) {
        super(isPodmanWorking);
    }
}
