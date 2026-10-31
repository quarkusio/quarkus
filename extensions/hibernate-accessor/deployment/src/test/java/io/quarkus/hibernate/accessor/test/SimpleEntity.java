package io.quarkus.hibernate.accessor.test;

import io.quarkus.hibernate.accessor.runtime.FieldAccess;
import io.quarkus.hibernate.accessor.runtime.ReflectionFreeFieldAccessor;

public class SimpleEntity {

    @ReflectionFreeFieldAccessor(access = FieldAccess.READ_WRITE)
    private String name;

    @ReflectionFreeFieldAccessor(access = FieldAccess.READ_WRITE)
    private int age;

    @ReflectionFreeFieldAccessor(access = FieldAccess.READ_WRITE)
    private boolean active;

    public SimpleEntity() {
    }

    public SimpleEntity(String name, int age, boolean active) {
        this.name = name;
        this.age = age;
        this.active = active;
    }

    public String getName() {
        return name;
    }

    public int getAge() {
        return age;
    }

    public boolean isActive() {
        return active;
    }
}
