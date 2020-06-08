package org.acme;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/working-dir")
public class WorkingDirResource {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return java.nio.file.Paths.get(".").toAbsolutePath().toString();
    }
}