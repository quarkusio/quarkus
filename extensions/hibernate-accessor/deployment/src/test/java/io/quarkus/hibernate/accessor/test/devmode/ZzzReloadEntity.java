package io.quarkus.hibernate.accessor.test.devmode;

import io.quarkus.hibernate.accessor.FieldAccess;
import io.quarkus.hibernate.accessor.GeneratedFieldAccessor;

public class ZzzReloadEntity {

    @GeneratedFieldAccessor(access = FieldAccess.READ_WRITE)
    private String name;

    public ZzzReloadEntity() {
    }

    public ZzzReloadEntity(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
