package io.quarkus.resteasy.reactive.server.test.resource.basic.resource;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;

public interface SubResourceLocatorBaseService extends SubResourceLocatorBaseCrudService<SubResourceLocatorOhaUserModel> {

    @GET
    @Produces("text/plain")
    @Path("data/ada/{user}")
    SubResourceLocatorOhaUserModel getUserDataByAdaId(
            @PathParam("user") String adaId);
}
