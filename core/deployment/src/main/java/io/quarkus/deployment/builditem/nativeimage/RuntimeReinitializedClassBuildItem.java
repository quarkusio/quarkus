package io.quarkus.deployment.builditem.nativeimage;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * A class that will be reinitialized at runtime in native mode. This will result in the static
 * initializer running twice.
 *
 * @deprecated Starting with Mandrel/GraalVM 23.1 for JDK 21 this is functionally the same with
 *             {@link RuntimeInitializedClassBuildItem}.
 */
@Deprecated(since = "3.18")
public final class RuntimeReinitializedClassBuildItem extends MultiBuildItem {

    private final String className;

    public RuntimeReinitializedClassBuildItem(String className) {
        this.className = className;
    }

    public String getClassName() {
        return className;
    }
}
