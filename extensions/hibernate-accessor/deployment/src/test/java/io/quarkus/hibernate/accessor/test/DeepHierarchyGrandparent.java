package io.quarkus.hibernate.accessor.test;

import io.quarkus.hibernate.accessor.runtime.FieldAccess;
import io.quarkus.hibernate.accessor.runtime.ReflectionFreeFieldAccessor;

public class DeepHierarchyGrandparent {

    @ReflectionFreeFieldAccessor(access = FieldAccess.READ_WRITE)
    private String grandparentField;

    public DeepHierarchyGrandparent() {
    }

    public DeepHierarchyGrandparent(String grandparentField) {
        this.grandparentField = grandparentField;
    }

    public String getGrandparentField() {
        return grandparentField;
    }
}
