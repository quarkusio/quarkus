package io.quarkus.hibernate.accessor.test.devmode;

import io.quarkus.hibernate.accessor.runtime.FieldAccess;
import io.quarkus.hibernate.accessor.runtime.ReflectionFreeFieldAccessor;

public class ZzzReloadEntity {

    @ReflectionFreeFieldAccessor(access = FieldAccess.READ_WRITE)
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
