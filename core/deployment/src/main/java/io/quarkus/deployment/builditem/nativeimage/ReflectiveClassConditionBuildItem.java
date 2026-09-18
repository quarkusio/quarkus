package io.quarkus.deployment.builditem.nativeimage;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Used to define a condition to register a class for reflection in native mode only when a specific type is reachable.
 * <br>
 * NOTE: The use of this build item requires the class to have already been registered through another
 * {@code Reflective*BuildItem}.
 *
 * @see ReflectiveClassBuildItem
 * @see ReflectiveFieldBuildItem
 * @see ReflectiveMethodBuildItem
 * @see ReflectiveHierarchyBuildItem
 */
public final class ReflectiveClassConditionBuildItem extends MultiBuildItem {

    private final String className;
    private final String typeReached;

    public ReflectiveClassConditionBuildItem(Class<?> className, String typeReached) {
        this(className.getName(), typeReached);
    }

    public ReflectiveClassConditionBuildItem(String className, String typeReached) {
        this.className = className;
        this.typeReached = typeReached;
    }

    public String getClassName() {
        return className;
    }

    public String getTypeReached() {
        return typeReached;
    }
}
