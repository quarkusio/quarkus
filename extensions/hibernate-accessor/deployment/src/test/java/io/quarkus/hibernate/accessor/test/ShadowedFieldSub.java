package io.quarkus.hibernate.accessor.test;

import io.quarkus.hibernate.accessor.FieldAccess;
import io.quarkus.hibernate.accessor.GeneratedFieldAccessor;

public class ShadowedFieldSub extends ShadowedFieldBase {

    @GeneratedFieldAccessor(access = FieldAccess.READ_WRITE)
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
