package io.quarkus.resteasy.reactive.server.test.resource.basic.resource;

import java.io.IOException;
import java.io.InputStream;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

@Path("/inputstream")
public class SpecialResourceStreamResource {
    @POST
    @Path("/test/{type}")
    public void test(InputStream is, @PathParam("type") final String type) throws IOException {

    }
}
