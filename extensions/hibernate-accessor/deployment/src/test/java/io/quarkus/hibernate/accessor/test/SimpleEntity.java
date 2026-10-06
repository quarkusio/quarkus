package io.quarkus.hibernate.accessor.test;

import io.quarkus.hibernate.accessor.runtime.ReflectionFreeAccessor;

public class SimpleEntity {

    @ReflectionFreeAccessor
    private String name;

    @ReflectionFreeAccessor
    private int age;

    @ReflectionFreeAccessor
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
