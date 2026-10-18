package org.jboss.resteasy.reactive.server.vertx.test.resource.basic.resource;

import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

@Path("/platform")
public interface SubResourceLocatorPlatformServiceResource {

    @Path("/users/{user}")
    SubResourceLocatorUserResource getUserService(
            @HeaderParam("entity") String entity,
            @HeaderParam("ticket") String ticket,
            @PathParam("user") String userId);
}
