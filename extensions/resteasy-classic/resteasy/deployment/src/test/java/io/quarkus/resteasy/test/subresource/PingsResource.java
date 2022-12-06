package io.quarkus.resteasy.test.subresource;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;

@Path("pings")
public class PingsResource {

    @Context
    ResourceContext resourceContext;

    @Path("do")
    public PingResource ping() {
        return resourceContext.getResource(PingResource.class);
    }

    @Path("super")
    public PingResource superPing() {
        return resourceContext.getResource(SuperPingResource.class);
    }

}
