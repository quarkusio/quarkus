package io.quarkus.hibernate.accessor.test;

import io.quarkus.hibernate.accessor.runtime.ReflectionFreeAccessor;

public class DeepHierarchyParent extends DeepHierarchyGrandparent {

    @ReflectionFreeAccessor
    private String parentField;

    public DeepHierarchyParent() {
    }

    public DeepHierarchyParent(String grandparentField, String parentField) {
        super(grandparentField);
        this.parentField = parentField;
    }

    public String getParentField() {
        return parentField;
    }
}
