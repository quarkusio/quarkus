package io.quarkus.hibernate.accessor.test;

import io.quarkus.hibernate.accessor.runtime.ReflectionFreeAccessor;

public class PrivateConstructorEntity {

    @ReflectionFreeAccessor
    private String name;

    @ReflectionFreeAccessor
    private PrivateConstructorEntity() {
    }

    @ReflectionFreeAccessor
    private PrivateConstructorEntity(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
