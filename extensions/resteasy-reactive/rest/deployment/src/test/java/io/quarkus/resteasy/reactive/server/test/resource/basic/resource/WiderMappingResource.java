package io.quarkus.resteasy.reactive.server.test.resource.basic.resource;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

@Path("/hello")
public class WiderMappingResource {
    @POST
    @Path("int")
    public int postInt(int val) {
        return val;
    }

    @POST
    @Path("boolean")
    public boolean postInt(boolean val) {
        return val;
    }
}
