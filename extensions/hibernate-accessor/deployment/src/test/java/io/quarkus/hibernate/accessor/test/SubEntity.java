package io.quarkus.hibernate.accessor.test;

import io.quarkus.hibernate.accessor.runtime.ReflectionFreeAccessor;

public class SubEntity extends BaseEntity {

    @ReflectionFreeAccessor
    private String extra;

    public SubEntity() {
    }

    public SubEntity(String name, int value, String extra) {
        super(name, value);
        this.extra = extra;
    }

    public String getExtra() {
        return extra;
    }
}
