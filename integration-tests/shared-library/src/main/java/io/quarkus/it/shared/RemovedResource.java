package io.quarkus.it.shared;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/removed")
public class RemovedResource {

    @GET
    public String removed() {
        return "removed";
    }
}
