package io.quarkus.resteasy.test.subresource;

import javax.ws.rs.Path;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;

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
