package io.quarkus.rest.server.test.simple;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/")
public class RootAResource {

    @GET
    @Path("a")
    public String a() {
        return "a";
    }

}
