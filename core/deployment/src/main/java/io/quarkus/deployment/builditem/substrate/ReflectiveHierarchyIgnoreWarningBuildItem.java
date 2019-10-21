package io.quarkus.deployment.builditem.substrate;

import org.jboss.jandex.DotName;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Used by {@link io.quarkus.deployment.builditem.substrate.ReflectiveHierarchyStep} to ignore reflection warning deliberately
 * 
 * @deprecated Use {@link io.quarkus.deployment.builditem.nativeimage.ReflectiveHierarchyIgnoreWarningBuildItem
 *             ReflectiveHierarchyIgnoreWarningBuildItem} instead.
 */
@Deprecated
public final class ReflectiveHierarchyIgnoreWarningBuildItem extends MultiBuildItem {

    private final DotName dotName;

    public ReflectiveHierarchyIgnoreWarningBuildItem(DotName dotName) {
        this.dotName = dotName;
    }

    public DotName getDotName() {
        return dotName;
    }

}
