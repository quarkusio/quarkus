package io.quarkus.hibernate.accessor.test;

import io.quarkus.hibernate.accessor.runtime.ReflectionFreeAccessor;

public class ShadowedFieldSub extends ShadowedFieldBase {

    @ReflectionFreeAccessor
    private String name;

    public ShadowedFieldSub() {
    }

    public ShadowedFieldSub(String baseName, int baseValue, String subName) {
        super(baseName, baseValue);
        this.name = subName;
    }

    public String getSubName() {
        return name;
    }
}
