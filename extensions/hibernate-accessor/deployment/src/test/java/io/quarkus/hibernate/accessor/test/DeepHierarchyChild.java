package io.quarkus.hibernate.accessor.test;

import io.quarkus.hibernate.accessor.FieldAccess;
import io.quarkus.hibernate.accessor.GeneratedFieldAccessor;

public class DeepHierarchyChild extends DeepHierarchyParent {

    @GeneratedFieldAccessor(access = FieldAccess.READ_WRITE)
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
