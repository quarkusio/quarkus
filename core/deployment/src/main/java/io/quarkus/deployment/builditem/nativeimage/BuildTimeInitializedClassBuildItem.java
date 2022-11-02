package io.quarkus.deployment.builditem.nativeimage;

import io.quarkus.builder.item.MultiBuildItem;
import io.smallrye.common.constraint.Assert;

/**
 * A class that can be initialized early in order to reduce time to first request.
 */
public final class BuildTimeInitializedClassBuildItem extends MultiBuildItem {
    private final String className;

    /**
     * Construct a new instance.
     *
     * @param className the class name (must not be {@code null})
     */
    public BuildTimeInitializedClassBuildItem(final String className) {
        this.className = Assert.checkNotNullParam("className", className);
    }

    /**
     * Get the class name.
     *
     * @return the class name (not {@code null})
     */
    public String getClassName() {
        return className;
    }
}
