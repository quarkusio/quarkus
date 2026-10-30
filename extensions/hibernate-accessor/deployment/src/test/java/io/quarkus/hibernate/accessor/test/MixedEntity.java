package io.quarkus.hibernate.accessor.test;

import io.quarkus.hibernate.accessor.FieldAccess;
import io.quarkus.hibernate.accessor.GeneratedFieldAccessor;
import io.quarkus.hibernate.accessor.GeneratedMethodAccessor;

public class MixedEntity {

    @GeneratedFieldAccessor(access = FieldAccess.READ_WRITE)
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

    @GeneratedMethodAccessor
    public String getDescription() {
        return description;
    }

    @GeneratedMethodAccessor
    public void setDescription(String description) {
        this.description = description;
    }
}
