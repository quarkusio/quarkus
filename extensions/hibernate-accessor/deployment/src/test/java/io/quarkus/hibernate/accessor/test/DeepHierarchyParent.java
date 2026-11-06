package io.quarkus.hibernate.accessor.test;

import io.quarkus.hibernate.accessor.FieldAccess;
import io.quarkus.hibernate.accessor.GeneratedFieldAccessor;

public class DeepHierarchyParent extends DeepHierarchyGrandparent {

    @GeneratedFieldAccessor(access = FieldAccess.READ_WRITE)
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
