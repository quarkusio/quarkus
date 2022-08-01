package org.jboss.resteasy.reactive.server.vertx.test.resource.basic.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

public class ResourceLocatorReceiver {

    @Path("/head")
    @GET
    public String get() {
        return this.getClass().getName();
    }
}
