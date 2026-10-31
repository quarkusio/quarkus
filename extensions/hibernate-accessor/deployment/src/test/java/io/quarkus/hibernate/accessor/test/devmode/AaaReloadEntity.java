package io.quarkus.hibernate.accessor.test.devmode;

import io.quarkus.hibernate.accessor.runtime.FieldAccess;
import io.quarkus.hibernate.accessor.runtime.ReflectionFreeFieldAccessor;

/**
 * Not part of the initial application: the dev mode test adds this source file on live reload.
 * Its name sorts before every other accessor host of that application, so adding it shifts the
 * positional class index of all the other hosts.
 */
public class AaaReloadEntity {

    @ReflectionFreeFieldAccessor(access = FieldAccess.READ_WRITE)
    private String name;

    public AaaReloadEntity() {
    }

    public AaaReloadEntity(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
