package org.jboss.shamrock.example.shared;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/shared")
public class SharedResource {

    @GET
    public String shared() {
        return "Shared Resource";
    }
}
