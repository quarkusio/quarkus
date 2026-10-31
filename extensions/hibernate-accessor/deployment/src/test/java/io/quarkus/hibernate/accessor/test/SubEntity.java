package io.quarkus.hibernate.accessor.test;

import io.quarkus.hibernate.accessor.runtime.FieldAccess;
import io.quarkus.hibernate.accessor.runtime.ReflectionFreeFieldAccessor;

public class SubEntity extends BaseEntity {

    @ReflectionFreeFieldAccessor(access = FieldAccess.READ_WRITE)
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
