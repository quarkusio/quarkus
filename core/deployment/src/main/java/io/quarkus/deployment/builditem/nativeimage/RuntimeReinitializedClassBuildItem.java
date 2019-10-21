package io.quarkus.deployment.builditem.nativeimage;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * A class that will be reinitialized at runtime in native mode. This will result in the static
 * initializer running twice.
 */
public final class RuntimeReinitializedClassBuildItem extends MultiBuildItem {

    private final String className;

    public RuntimeReinitializedClassBuildItem(String className) {
        this.className = className;
    }

    public String getClassName() {
        return className;
    }
}
