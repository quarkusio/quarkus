package io.quarkus.hibernate.accessor.test;

import io.quarkus.hibernate.accessor.runtime.ReflectionFreeAccessor;

public class DeepHierarchyChild extends DeepHierarchyParent {

    @ReflectionFreeAccessor
    private String childField;

    public DeepHierarchyChild() {
    }

    public DeepHierarchyChild(String grandparentField, String parentField, String childField) {
        super(grandparentField, parentField);
        this.childField = childField;
    }

    public String getChildField() {
        return childField;
    }
}
