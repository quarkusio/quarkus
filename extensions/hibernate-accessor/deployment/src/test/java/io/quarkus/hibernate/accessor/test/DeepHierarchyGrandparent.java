package io.quarkus.hibernate.accessor.test;

import io.quarkus.hibernate.accessor.runtime.ReflectionFreeAccessor;

public class DeepHierarchyGrandparent {

    @ReflectionFreeAccessor
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
