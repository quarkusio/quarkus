package org.jboss.resteasy.reactive.server.vertx.test.matching;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

@Path("/regex")
public class RegexResource {

    @GET
    @Path("/{pin:[A-Z0-9]{4}}")
    public String hello(@PathParam("pin") String name) {
        return "pin " + name;
    }

}
