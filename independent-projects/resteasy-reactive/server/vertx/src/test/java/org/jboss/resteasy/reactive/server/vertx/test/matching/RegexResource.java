package org.jboss.resteasy.reactive.server.vertx.test.matching;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

@Path("/regex")
public class RegexResource {

    @GET
    @Path("/{  1p-_in.  :  [A-Z0-9]{4}  }")
    public String hello(@PathParam("1p-_in.") String name) {
        return "pin " + name;
    }

}
