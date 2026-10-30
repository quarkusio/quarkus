package io.quarkus.hibernate.accessor.test;

import io.quarkus.hibernate.accessor.FieldAccess;
import io.quarkus.hibernate.accessor.GeneratedFieldAccessor;

public class SubEntity extends BaseEntity {

    @GeneratedFieldAccessor(access = FieldAccess.READ_WRITE)
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
