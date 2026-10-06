package io.quarkus.hibernate.accessor.test;

import io.quarkus.hibernate.accessor.runtime.ReflectionFreeAccessor;

public class MethodEntity {

    private String value;
    private int count;

    public MethodEntity() {
    }

    public MethodEntity(String value, int count) {
        this.value = value;
        this.count = count;
    }

    @ReflectionFreeAccessor
    public String getValue() {
        return value;
    }

    @ReflectionFreeAccessor
    public void setValue(String value) {
        this.value = value;
    }

    @ReflectionFreeAccessor
    public void setValue(Integer value) {
        this.count = value;
    }

    @ReflectionFreeAccessor
    public int getCount() {
        return count;
    }

    @ReflectionFreeAccessor
    public void setCount(int count) {
        this.count = count;
    }
}
