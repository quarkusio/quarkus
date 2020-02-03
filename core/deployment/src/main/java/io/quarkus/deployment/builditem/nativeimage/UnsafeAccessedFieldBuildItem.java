package io.quarkus.deployment.builditem.nativeimage;

import java.util.Comparator;

import io.quarkus.builder.item.MultiBuildItem;

public final class UnsafeAccessedFieldBuildItem extends MultiBuildItem implements Comparable<UnsafeAccessedFieldBuildItem> {

    private static final Comparator<UnsafeAccessedFieldBuildItem> COMPARATOR = Comparator
            .comparing((UnsafeAccessedFieldBuildItem item) -> item.declaringClass)
            .thenComparing(item -> item.fieldName);

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

    @Override
    public int compareTo(UnsafeAccessedFieldBuildItem other) {
        return COMPARATOR.compare(this, other);
    }
}
