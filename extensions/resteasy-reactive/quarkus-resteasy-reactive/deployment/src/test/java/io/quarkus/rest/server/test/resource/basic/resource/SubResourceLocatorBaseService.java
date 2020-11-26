package io.quarkus.rest.server.test.resource.basic.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

public interface SubResourceLocatorBaseService extends SubResourceLocatorBaseCrudService<SubResourceLocatorOhaUserModel> {

    @GET
    @Produces("text/plain")
    @Path("data/ada/{user}")
    SubResourceLocatorOhaUserModel getUserDataByAdaId(
            @PathParam("user") String adaId);
}
