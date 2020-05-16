package io.quarkus.rest.data.panache.runtime.hal;

public class HalEntityWrapper {

    private final Object entity;

    public HalEntityWrapper(Object entity) {
        this.entity = entity;
    }

    public Object getEntity() {
        return entity;
    }
}
