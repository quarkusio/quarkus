package io.quarkus.deployment.builditem;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.deployment.IsContainerRuntimeWorking;

/**
 * An abstract build item indicating the availability and status of a container runtime.
 * <p>
 * Subclasses provide status for specific container runtimes (e.g., Docker, Podman).
 * It uses an {@link IsContainerRuntimeWorking} instance to perform the actual check
 * and caches the result.
 */
public abstract class ContainerRuntimeStatusBuildItem extends SimpleBuildItem {
    private final IsContainerRuntimeWorking isContainerRuntimeWorking;
    private Boolean cachedStatus;

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
