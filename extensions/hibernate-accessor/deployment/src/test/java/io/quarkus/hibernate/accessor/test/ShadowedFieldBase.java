package io.quarkus.hibernate.accessor.test;

import io.quarkus.hibernate.accessor.runtime.FieldAccess;
import io.quarkus.hibernate.accessor.runtime.ReflectionFreeFieldAccessor;

public class ShadowedFieldBase {

    @ReflectionFreeFieldAccessor(access = FieldAccess.READ_WRITE)
    private String name;

    @ReflectionFreeFieldAccessor(access = FieldAccess.READ_WRITE)
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
