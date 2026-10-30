package io.quarkus.hibernate.accessor.test;

import io.quarkus.hibernate.accessor.runtime.FieldAccess;
import io.quarkus.hibernate.accessor.runtime.ReflectionFreeConstructorAccessor;
import io.quarkus.hibernate.accessor.runtime.ReflectionFreeFieldAccessor;

public class PrivateConstructorEntity {

    @ReflectionFreeFieldAccessor(access = FieldAccess.READ_WRITE)
    private String name;

    @ReflectionFreeConstructorAccessor
    private PrivateConstructorEntity() {
    }

    @ReflectionFreeConstructorAccessor
    private PrivateConstructorEntity(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
