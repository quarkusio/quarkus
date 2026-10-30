package io.quarkus.hibernate.accessor.test;

import io.quarkus.hibernate.accessor.runtime.FieldAccess;
import io.quarkus.hibernate.accessor.runtime.ReflectionFreeFieldAccessor;

public class DeepHierarchyChild extends DeepHierarchyParent {

    @ReflectionFreeFieldAccessor(access = FieldAccess.READ_WRITE)
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
