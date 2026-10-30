package io.quarkus.hibernate.accessor.test;

import io.quarkus.hibernate.accessor.runtime.ReflectionFreeMethodAccessor;

public class MethodEntity {

    private String value;
    private int count;

    public MethodEntity() {
    }

    public MethodEntity(String value, int count) {
        this.value = value;
        this.count = count;
    }

    @ReflectionFreeMethodAccessor
    public String getValue() {
        return value;
    }

    @ReflectionFreeMethodAccessor
    public void setValue(String value) {
        this.value = value;
    }

    @ReflectionFreeMethodAccessor
    public void setValue(Integer value) {
        this.count = value;
    }

    @ReflectionFreeMethodAccessor
    public int getCount() {
        return count;
    }

    @ReflectionFreeMethodAccessor
    public void setCount(int count) {
        this.count = count;
    }
}
