package io.quarkus.hibernate.accessor.test;

import io.quarkus.hibernate.accessor.runtime.FieldAccess;
import io.quarkus.hibernate.accessor.runtime.ReflectionFreeFieldAccessor;

class PackagePrivateEntity {

    @ReflectionFreeFieldAccessor(access = FieldAccess.READ_WRITE)
    private String name;

    @ReflectionFreeFieldAccessor(access = FieldAccess.READ_WRITE)
    private int value;

    public PackagePrivateEntity() {
    }

    public PackagePrivateEntity(String name, int value) {
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
