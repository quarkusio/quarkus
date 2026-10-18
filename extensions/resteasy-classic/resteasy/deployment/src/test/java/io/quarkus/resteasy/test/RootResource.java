package io.quarkus.resteasy.test;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/")
public class RootResource {

    @GET
    public String root() {
        return "Root Resource";
    }
}
