package io.quarkus.hibernate.accessor.test;

import io.quarkus.hibernate.accessor.runtime.ReflectionFreeAccessor;

public class MixedEntity {

    @ReflectionFreeAccessor
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

    @ReflectionFreeAccessor
    public String getDescription() {
        return description;
    }

    @ReflectionFreeAccessor
    public void setDescription(String description) {
        this.description = description;
    }
}
