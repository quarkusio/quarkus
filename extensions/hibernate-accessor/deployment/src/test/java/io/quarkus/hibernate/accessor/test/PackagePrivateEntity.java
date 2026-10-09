package io.quarkus.hibernate.accessor.test;

import io.quarkus.hibernate.accessor.runtime.ReflectionFreeAccessor;

class PackagePrivateEntity {

    @ReflectionFreeAccessor
    private String name;

    @ReflectionFreeAccessor
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
