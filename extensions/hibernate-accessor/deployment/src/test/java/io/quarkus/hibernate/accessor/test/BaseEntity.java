package io.quarkus.hibernate.accessor.test;

import io.quarkus.hibernate.accessor.runtime.ReflectionFreeAccessor;

public class BaseEntity {

    @ReflectionFreeAccessor
    private String name;

    @ReflectionFreeAccessor
    private int value;

    public BaseEntity() {
    }

    public BaseEntity(String name, int value) {
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
