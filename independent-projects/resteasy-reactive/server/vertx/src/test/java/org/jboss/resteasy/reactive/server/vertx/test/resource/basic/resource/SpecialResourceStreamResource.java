package org.jboss.resteasy.reactive.server.vertx.test.resource.basic.resource;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import java.io.IOException;
import java.io.InputStream;

@Path("/inputstream")
public class SpecialResourceStreamResource {
    @POST
    @Path("/test/{type}")
    public void test(InputStream is, @PathParam("type") final String type) throws IOException {

    }
}
