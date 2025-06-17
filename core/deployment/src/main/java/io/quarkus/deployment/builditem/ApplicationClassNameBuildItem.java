package io.quarkus.deployment.builditem;

import io.quarkus.builder.item.SimpleBuildItem;


/**
 * This class get the class name for an ApplicationBuildItem.
 *
 */
public final class ApplicationClassNameBuildItem extends SimpleBuildItem {
    private final String className;

    public ApplicationClassNameBuildItem(String className) {
        this.className = className;
    }

    public String getClassName() {
        return className;
    }
}
