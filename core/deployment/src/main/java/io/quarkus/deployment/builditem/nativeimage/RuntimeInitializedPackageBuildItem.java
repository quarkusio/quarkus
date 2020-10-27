package io.quarkus.deployment.builditem.nativeimage;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * A package that will be initialized at runtime in native mode.
 */
public final class RuntimeInitializedPackageBuildItem extends MultiBuildItem {

    private final String packageName;

    public RuntimeInitializedPackageBuildItem(String packageName) {
        this.packageName = packageName;
    }

    public String getPackageName() {
        return packageName;
    }
}
