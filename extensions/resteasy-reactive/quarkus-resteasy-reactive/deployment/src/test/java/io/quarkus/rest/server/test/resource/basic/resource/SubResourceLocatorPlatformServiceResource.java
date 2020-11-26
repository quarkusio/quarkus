package io.quarkus.rest.server.test.resource.basic.resource;

import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

@Path("/platform")
public interface SubResourceLocatorPlatformServiceResource {

    @Path("/users/{user}")
    SubResourceLocatorUserResource getUserService(
            @HeaderParam("entity") String entity,
            @HeaderParam("ticket") String ticket,
            @PathParam("user") String userId);
}
