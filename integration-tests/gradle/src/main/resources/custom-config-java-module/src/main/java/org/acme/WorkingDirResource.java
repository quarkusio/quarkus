package org.acme;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/working-dir")
public class WorkingDirResource {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return java.nio.file.Paths.get(".").toAbsolutePath().toString();
    }
}
