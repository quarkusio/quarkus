package io.quarkus.rest.server.test.resource.basic.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

public class ScanSubresource {
    @Path("doit")
    @GET
    @Produces("text/plain")
    public String get() {
        return "subresource-doit";
    }
}
