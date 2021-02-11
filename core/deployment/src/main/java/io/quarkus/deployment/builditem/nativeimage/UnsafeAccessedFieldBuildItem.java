package io.quarkus.deployment.builditem.nativeimage;

import io.quarkus.builder.item.MultiBuildItem;

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
