package io.quarkus.deployment.builditem;

import io.quarkus.deployment.IsDockerWorking;

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
