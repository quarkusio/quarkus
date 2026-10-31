package io.quarkus.hibernate.accessor.test;

import io.quarkus.hibernate.accessor.runtime.FieldAccess;
import io.quarkus.hibernate.accessor.runtime.ReflectionFreeFieldAccessor;

public class ShadowedFieldSub extends ShadowedFieldBase {

    @ReflectionFreeFieldAccessor(access = FieldAccess.READ_WRITE)
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
