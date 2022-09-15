package io.quarkus.it.resources;

import java.io.IOException;
import java.net.URL;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;

@Path("/resources")
public class ResourcesResource {
    @GET
    @Path("/{path:.+}")
    public Response resource(@PathParam("path") String path) throws IOException {
        final URL url = getClass().getClassLoader().getResource(path);
        if (url == null) {
            return Response.status(404).build();
        } else {
            return Response.ok(url.openStream()).build();
        }
    }
}
