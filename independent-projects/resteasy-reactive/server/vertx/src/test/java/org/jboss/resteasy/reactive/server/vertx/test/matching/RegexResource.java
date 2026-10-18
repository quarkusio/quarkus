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

    @GET
    @Path("{p1}/{p2:.*}/{p3:.*}")
    public String noliteral(@PathParam("p1") String p1, @PathParam("p2") String p2, @PathParam("p3") String p3) {
        return "plain:" + p1 + "/" + p2 + "/" + p3;
    }

    @GET
    @Path("{p1}/literal/{p2:.*}")
    public String literal(@PathParam("p1") String p1, @PathParam("p2") String p2) {
        return "literal:" + p1 + "/" + p2;
    }
}
