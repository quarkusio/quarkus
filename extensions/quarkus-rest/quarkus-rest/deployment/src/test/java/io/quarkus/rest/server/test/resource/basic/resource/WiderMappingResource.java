package io.quarkus.rest.server.test.resource.basic.resource;

import javax.ws.rs.POST;
import javax.ws.rs.Path;

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
