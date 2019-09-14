package io.quarkus.it.cxf;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/rest")
public class RestResource {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String get() {
        return "get success";
    }
}
