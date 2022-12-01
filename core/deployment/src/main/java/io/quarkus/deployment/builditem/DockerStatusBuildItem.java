package io.quarkus.deployment.builditem;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.deployment.IsDockerWorking;

public final class DockerStatusBuildItem extends SimpleBuildItem {

    private final IsDockerWorking isDockerWorking;
    private Boolean cachedStatus;

    public DockerStatusBuildItem(IsDockerWorking isDockerWorking) {
        this.isDockerWorking = isDockerWorking;
    }

    public synchronized boolean isDockerAvailable() {
        if (cachedStatus == null) {
            cachedStatus = isDockerWorking.getAsBoolean();
        }
        return cachedStatus;
    }
}
