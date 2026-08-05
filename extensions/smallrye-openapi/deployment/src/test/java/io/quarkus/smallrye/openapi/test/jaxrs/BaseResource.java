package io.quarkus.smallrye.openapi.test.jaxrs;

import java.util.List;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

public abstract class BaseResource<D> {

    @GET
    @Path("/list")
    public List<D> list() {
        return List.of();
    }

    @POST
    @Path("/create")
    public D create(D entity) {
        return entity;
    }

    @GET
    @Path("/count")
    public long count() {
        return 0;
    }
}
