package io.quarkus.rest.server.test.resource.basic.resource;

import java.io.IOException;
import java.io.InputStream;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

@Path("/inputstream")
public class SpecialResourceStreamResource {
    @POST
    @Path("/test/{type}")
    public void test(InputStream is, @PathParam("type") final String type) throws IOException {

    }
}
