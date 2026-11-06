package io.quarkus.hibernate.accessor.test;

import io.quarkus.hibernate.accessor.GeneratedMethodAccessor;

public class MethodEntity {

    private String value;
    private int count;

    public MethodEntity() {
    }

    public MethodEntity(String value, int count) {
        this.value = value;
        this.count = count;
    }

    @GeneratedMethodAccessor
    public String getValue() {
        return value;
    }

    @GeneratedMethodAccessor
    public void setValue(String value) {
        this.value = value;
    }

    @GeneratedMethodAccessor
    public void setValue(Integer value) {
        this.count = value;
    }

    @GeneratedMethodAccessor
    public int getCount() {
        return count;
    }

    @GeneratedMethodAccessor
    public void setCount(int count) {
        this.count = count;
    }
}
