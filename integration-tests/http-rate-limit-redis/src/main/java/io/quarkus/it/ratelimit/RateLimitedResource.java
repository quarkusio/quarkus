package io.quarkus.it.ratelimit;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/")
public class RateLimitedResource {

    @GET
    @Path("/api/hello")
    public String api() {
        return "hello";
    }

    @GET
    @Path("/lease/hello")
    public String lease() {
        return "hello";
    }
}
