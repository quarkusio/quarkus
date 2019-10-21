package io.quarkus.deployment.builditem.substrate;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * @deprecated Use {@link io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem
 *             RuntimeInitializedClassBuildItem} instead.
 */
@Deprecated
public final class RuntimeInitializedClassBuildItem extends MultiBuildItem {

    private final String className;

    public RuntimeInitializedClassBuildItem(String className) {
        this.className = className;
    }

    public String getClassName() {
        return className;
    }
}
