package io.quarkus.deployment.builditem.nativeimage;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Represents fields that are accessed using unsafe operations.
 * This build item provides information about the class and field names
 * that require special handling during the build process.
 */
public final class UnsafeAccessedFieldBuildItem extends MultiBuildItem {

    final String declaringClass;
    final String fieldName;

    public UnsafeAccessedFieldBuildItem(String declaringClass, String fieldName) {
        this.declaringClass = declaringClass;
        this.fieldName = fieldName;
    }

    public String getDeclaringClass() {
        return declaringClass;
    }

    public String getFieldName() {
        return fieldName;
    }
}
