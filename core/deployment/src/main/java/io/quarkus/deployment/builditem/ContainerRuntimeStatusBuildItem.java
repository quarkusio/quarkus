package io.quarkus.deployment.builditem;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.deployment.IsContainerRuntimeWorking;

public abstract class ContainerRuntimeStatusBuildItem extends SimpleBuildItem {
    private final IsContainerRuntimeWorking isContainerRuntimeWorking;
    private volatile Boolean cachedStatus;

    protected ContainerRuntimeStatusBuildItem(IsContainerRuntimeWorking isContainerRuntimeWorking) {
        this.isContainerRuntimeWorking = isContainerRuntimeWorking;
    }

    public boolean isContainerRuntimeAvailable() {
        if (cachedStatus == null) {
            synchronized (this) {
                if (cachedStatus == null) {
                    cachedStatus = isContainerRuntimeWorking.getAsBoolean();
                }
            }
        }

        return cachedStatus;
    }
}
