package io.quarkus.hibernate.accessor.test;

import io.quarkus.hibernate.accessor.FieldAccess;
import io.quarkus.hibernate.accessor.GeneratedFieldAccessor;

public class BaseEntity {

    @GeneratedFieldAccessor(access = FieldAccess.READ_WRITE)
    private String name;

    @GeneratedFieldAccessor(access = FieldAccess.READ_WRITE)
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
