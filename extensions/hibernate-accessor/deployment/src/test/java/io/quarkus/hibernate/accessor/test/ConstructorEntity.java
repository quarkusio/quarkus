package io.quarkus.hibernate.accessor.test;

import io.quarkus.hibernate.accessor.runtime.ReflectionFreeAccessor;

public class ConstructorEntity {

    @ReflectionFreeAccessor
    private String name;

    @ReflectionFreeAccessor
    private int value;

    @ReflectionFreeAccessor
    public ConstructorEntity() {
    }

    @ReflectionFreeAccessor
    public ConstructorEntity(String name, int value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public int getValue() {
        return value;
    }
}
