package io.quarkus.deployment.builditem;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.deployment.IsContainerRuntimeWorking;

/**
 * A build item that represents the status of a container runtime.
 * <p>
 * This abstract class provides a mechanism to check if a container runtime
 * is available and caches the result for subsequent calls.
 * </p>
 */
public abstract class ContainerRuntimeStatusBuildItem extends SimpleBuildItem {

    /**
     * A functional interface ({@link java.util.function.BooleanSupplier}) used to determine if the container runtime is
     * working.
     */
    private final IsContainerRuntimeWorking isContainerRuntimeWorking;

    /**
     * A cached status indicating whether the container runtime is available.
     * This value is computed lazily and stored for future use.
     */
    private volatile Boolean cachedStatus;

    /**
     * Constructs a new {@link ContainerRuntimeStatusBuildItem}.
     *
     * @param isContainerRuntimeWorking a functional interface to check the container runtime status
     */
    protected ContainerRuntimeStatusBuildItem(IsContainerRuntimeWorking isContainerRuntimeWorking) {
        this.isContainerRuntimeWorking = isContainerRuntimeWorking;
    }

    /**
     * Checks if the container runtime is available.
     * <p>
     * This method uses the {@link IsContainerRuntimeWorking} interface to determine
     * the runtime status and caches the result for subsequent calls.
     * </p>
     *
     * @return {@code true} if the container runtime is available, {@code false} otherwise
     */
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