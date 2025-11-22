package io.quarkus.deployment.builditem;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Holds the name of the application class.
 */
public final class ApplicationClassNameBuildItem extends SimpleBuildItem {
    /**
     * The name of the application class.
     */
    private final String className;

    /**
     * Constructs a new {@link ApplicationClassNameBuildItem}.
     *
     * @param className the name of the application class
     */

    public ApplicationClassNameBuildItem(String className) {
        this.className = className;
    }

    /**
     * Returns the name of the application class.
     *
     * @return the application class name
     */
    public String getClassName() {
        return className;
    }
}
