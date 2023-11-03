package io.quarkus.maven.it;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/added")
public class AddedRestEndpoint {

    @GET
    public String added() {
        return "added endpoint";
    }
}
