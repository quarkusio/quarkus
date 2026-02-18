package io.quarkus.arc.deployment;

import org.jboss.jandex.DotName;

import io.quarkus.builder.item.MultiBuildItem;

public final class FieldMockBuildItem extends MultiBuildItem {
    private final DotName declaringClass;
    private final String fieldName;
    private final boolean deepMocks;

    public FieldMockBuildItem(DotName declaringClass, String fieldName, boolean deepMocks) {
        this.declaringClass = declaringClass;
        this.fieldName = fieldName;
        this.deepMocks = deepMocks;
    }

    public DotName getDeclaringClass() {
        return declaringClass;
    }

    public String getFieldName() {
        return fieldName;
    }

    public boolean isDeepMocks() {
        return deepMocks;
    }
}
