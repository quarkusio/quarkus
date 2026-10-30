package io.quarkus.hibernate.accessor.test;

import io.quarkus.hibernate.accessor.FieldAccess;
import io.quarkus.hibernate.accessor.GeneratedConstructorAccessor;
import io.quarkus.hibernate.accessor.GeneratedFieldAccessor;

public class PrivateConstructorEntity {

    @GeneratedFieldAccessor(access = FieldAccess.READ_WRITE)
    private String name;

    @GeneratedConstructorAccessor
    private PrivateConstructorEntity() {
    }

    @GeneratedConstructorAccessor
    private PrivateConstructorEntity(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
