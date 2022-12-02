package io.quarkus.deployment.builditem;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Indicates that the given class should be instantiated with the constructor with the most parameters
 * when the object is bytecode recorded.
 *
 * An alternative to {@link RecordableConstructorBuildItem} for when the objects cannot be annotated
 */
public final class RecordableConstructorBuildItem extends MultiBuildItem {

    private final Class<?> clazz;

    public RecordableConstructorBuildItem(Class<?> clazz) {
        this.clazz = clazz;
    }

    public Class<?> getClazz() {
        return clazz;
    }
}
