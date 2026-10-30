package io.quarkus.hibernate.accessor.test;

import io.quarkus.hibernate.accessor.FieldAccess;
import io.quarkus.hibernate.accessor.GeneratedFieldAccessor;

public class ShadowedFieldBase {

    @GeneratedFieldAccessor(access = FieldAccess.READ_WRITE)
    private String name;

    @GeneratedFieldAccessor(access = FieldAccess.READ_WRITE)
    private int value;

    public ShadowedFieldBase() {
    }

    public ShadowedFieldBase(String name, int value) {
        this.name = name;
        this.value = value;
    }

    public String getBaseName() {
        return name;
    }

    public int getBaseValue() {
        return value;
    }
}
