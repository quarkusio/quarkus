package io.quarkus.hibernate.accessor.test.devmode;

import io.quarkus.hibernate.accessor.runtime.FieldAccess;
import io.quarkus.hibernate.accessor.runtime.ReflectionFreeFieldAccessor;

public class MmmReloadEntity {

    @ReflectionFreeFieldAccessor(access = FieldAccess.READ_WRITE)
    private String name;

    public MmmReloadEntity() {
    }

    public MmmReloadEntity(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
