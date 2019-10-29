package io.quarkus.deployment.builditem.substrate;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * A class that will be reinitialized at runtime by Substrate. This will result in the static
 * initializer running twice.
 * 
 * @deprecated Use {@link io.quarkus.deployment.builditem.nativeimage.RuntimeReinitializedClassBuildItem
 *             RuntimeReinitializedClassBuildItem} instead.
 */
@Deprecated
public final class RuntimeReinitializedClassBuildItem extends MultiBuildItem {

    private final String className;

    public RuntimeReinitializedClassBuildItem(String className) {
        this.className = className;
    }

    public String getClassName() {
        return className;
    }
}
