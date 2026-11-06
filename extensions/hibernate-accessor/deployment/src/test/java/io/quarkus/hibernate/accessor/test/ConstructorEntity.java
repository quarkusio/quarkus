package io.quarkus.hibernate.accessor.test;

import io.quarkus.hibernate.accessor.FieldAccess;
import io.quarkus.hibernate.accessor.GeneratedConstructorAccessor;
import io.quarkus.hibernate.accessor.GeneratedFieldAccessor;

public class ConstructorEntity {

    @GeneratedFieldAccessor(access = FieldAccess.READ_WRITE)
    private String name;

    @GeneratedFieldAccessor(access = FieldAccess.READ_WRITE)
    private int value;

    @GeneratedConstructorAccessor
    public ConstructorEntity() {
    }

    @GeneratedConstructorAccessor
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
