package io.quarkus.hibernate.accessor.test;

import io.quarkus.hibernate.accessor.FieldAccess;
import io.quarkus.hibernate.accessor.GeneratedFieldAccessor;

public class DeepHierarchyGrandparent {

    @GeneratedFieldAccessor(access = FieldAccess.READ_WRITE)
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
