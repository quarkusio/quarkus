package io.quarkus.deployment.builditem.nativeimage;

import org.jboss.jandex.DotName;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Used by {@link io.quarkus.deployment.steps.ReflectiveHierarchyStep} to ignore reflection warning deliberately
 */
public final class ReflectiveHierarchyIgnoreWarningBuildItem extends MultiBuildItem {

    private final DotName dotName;

    public ReflectiveHierarchyIgnoreWarningBuildItem(DotName dotName) {
        this.dotName = dotName;
    }

    public DotName getDotName() {
        return dotName;
    }

}
