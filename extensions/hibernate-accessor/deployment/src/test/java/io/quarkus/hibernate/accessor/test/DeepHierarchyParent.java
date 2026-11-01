package io.quarkus.hibernate.accessor.test;

import io.quarkus.hibernate.accessor.runtime.FieldAccess;
import io.quarkus.hibernate.accessor.runtime.ReflectionFreeFieldAccessor;

public class DeepHierarchyParent extends DeepHierarchyGrandparent {

    @ReflectionFreeFieldAccessor(access = FieldAccess.READ_WRITE)
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
