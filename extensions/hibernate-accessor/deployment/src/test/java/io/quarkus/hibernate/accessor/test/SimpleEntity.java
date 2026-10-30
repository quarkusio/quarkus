package io.quarkus.hibernate.accessor.test;

import io.quarkus.hibernate.accessor.FieldAccess;
import io.quarkus.hibernate.accessor.GeneratedFieldAccessor;

public class SimpleEntity {

    @GeneratedFieldAccessor(access = FieldAccess.READ_WRITE)
    private String name;

    @GeneratedFieldAccessor(access = FieldAccess.READ_WRITE)
    private int age;

    @GeneratedFieldAccessor(access = FieldAccess.READ_WRITE)
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
