package io.quarkus.deployment.builditem;

import io.quarkus.builder.item.MultiBuildItem;
import io.smallrye.common.constraint.NotNull;

/**
 * A build item indicating that a specific dev service, identified by a unique name,
 * attempted to start but failed.
 * This can be used by other extensions to implement fallback logic or any other action.
 */
public final class FailedDevServiceBuildItem extends MultiBuildItem {
    private final String serviceName;
    private final Throwable cause;

    public FailedDevServiceBuildItem(@NotNull String serviceName) {
        this.serviceName = serviceName;
        this.cause = null;
    }

    public FailedDevServiceBuildItem(String serviceName, Throwable cause) {
        this.serviceName = serviceName;
        this.cause = cause;
    }

    public String getServiceName() {
        return serviceName;
    }

    public Throwable getCause() {
        return cause;
    }
}