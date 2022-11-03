package io.quarkus.deployment.builditem;

import io.quarkus.builder.item.MultiBuildItem;
import io.smallrye.common.constraint.Assert;

/**
 * Class to be preloaded in static initialization phase of Quarkus
 */
public class PreloadClassBuildItem extends MultiBuildItem {
    private final String className;

    /**
     * Construct a new instance.
     *
     * @param className the class name (must not be {@code null})
     */
    public PreloadClassBuildItem(final String className) {
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