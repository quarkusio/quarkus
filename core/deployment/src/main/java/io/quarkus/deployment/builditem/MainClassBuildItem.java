package io.quarkus.deployment.builditem;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Holds the name of the main class for the application.
 */
public final class MainClassBuildItem extends SimpleBuildItem {

    public final String className;

    public MainClassBuildItem(String className) {
        this.className = className;
    }

    public String getClassName() {
        return className;
    }
}
