package io.quarkus.it.shared;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/removed")
public class RemovedResource {

    @GET
    public String removed() {
        return "removed";
    }
}
