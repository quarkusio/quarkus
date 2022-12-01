package io.quarkus.deployment.builditem.nativeimage;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Forces classes that have been registered for reflection using weak semantics, to revert to normal reflection registration
 * semantics.
 * Essentially if this build item is used for a class that has been registered with {@link ReflectiveClassBuildItem},
 * the {@code weak} field of that class is effectively false, no matter what value was supplied when creating
 * {@code ReflectiveClassBuildItem}
 */
public final class ForceNonWeakReflectiveClassBuildItem extends MultiBuildItem {

    private final String className;

    public ForceNonWeakReflectiveClassBuildItem(String className) {
        this.className = className;
    }

    public String getClassName() {
        return className;
    }
}
