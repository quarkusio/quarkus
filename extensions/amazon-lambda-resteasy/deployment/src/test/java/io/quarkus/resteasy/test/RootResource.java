package io.quarkus.resteasy.test;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/")
public class RootResource {

    @GET
    public String root() {
        return "Root Resource";
    }
}
