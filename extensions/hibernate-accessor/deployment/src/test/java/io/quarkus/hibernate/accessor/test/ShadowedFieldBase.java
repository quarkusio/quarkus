package io.quarkus.hibernate.accessor.test;

import io.quarkus.hibernate.accessor.runtime.ReflectionFreeAccessor;

public class ShadowedFieldBase {

    @ReflectionFreeAccessor
    private String name;

    @ReflectionFreeAccessor
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
