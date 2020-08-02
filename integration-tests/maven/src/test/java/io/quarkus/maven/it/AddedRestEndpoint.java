package io.quarkus.maven.it;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/added")
public class AddedRestEndpoint {

    @GET
    public String added() {
        return "added endpoint";
    }
}
