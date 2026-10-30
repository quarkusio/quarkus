package io.quarkus.hibernate.accessor.test;

import io.quarkus.hibernate.accessor.runtime.FieldAccess;
import io.quarkus.hibernate.accessor.runtime.ReflectionFreeFieldAccessor;
import io.quarkus.hibernate.accessor.runtime.ReflectionFreeMethodAccessor;

public class MixedEntity {

    @ReflectionFreeFieldAccessor(access = FieldAccess.READ_WRITE)
    private String name;

    private String description;

    public MixedEntity() {
    }

    public MixedEntity(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    @ReflectionFreeMethodAccessor
    public String getDescription() {
        return description;
    }

    @ReflectionFreeMethodAccessor
    public void setDescription(String description) {
        this.description = description;
    }
}
