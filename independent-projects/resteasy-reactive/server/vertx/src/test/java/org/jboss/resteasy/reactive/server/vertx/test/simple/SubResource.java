package org.jboss.resteasy.reactive.server.vertx.test.simple;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

public class SubResource {

    @GET
    public String sub() {
        return "sub";
    }

    @GET
    @Path("otherSub")
    public String otherPath() {
        return "otherSub";
    }
}
