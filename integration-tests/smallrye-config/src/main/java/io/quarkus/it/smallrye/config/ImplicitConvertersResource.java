package io.quarkus.it.smallrye.config;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

@Path("/implicit/converters")
public class ImplicitConvertersResource {
    @Inject
    ImplicitConverters implicitConverters;

    @GET
    @Path("/optional")
    public Response optional() {
        return Response.ok(implicitConverters.optional().get().getValue()).build();
    }

    @GET
    @Path("/list")
    public Response list() {
        return Response.ok(implicitConverters.list().get(0).getValue()).build();
    }

    @GET
    @Path("/map")
    public Response map() {
        return Response.ok(implicitConverters.map().get("key").getValue()).build();
    }
}
