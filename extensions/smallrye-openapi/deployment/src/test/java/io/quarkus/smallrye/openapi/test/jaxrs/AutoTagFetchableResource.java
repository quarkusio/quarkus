package io.quarkus.smallrye.openapi.test.jaxrs;

import java.util.List;

import jakarta.ws.rs.GET;

public abstract class AutoTagFetchableResource<T> implements AbstractAutoTagResource<T> {

    @GET
    abstract List<T> getAll();

    @Override
    public T getById(long id) {
        return null;
    }
}
