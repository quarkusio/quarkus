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

    @GET
    @Path("/subscription/hello")
    public String subscription() {
        return "hello";
    }

    @GET
    @Path("/subscription-lease/hello")
    public String subscriptionLease() {
        return "hello";
    }
}
