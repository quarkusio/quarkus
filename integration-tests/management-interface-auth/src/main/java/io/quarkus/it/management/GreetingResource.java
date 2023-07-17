package io.quarkus.it.management;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/service")
public class GreetingResource {

    @GET
    @Path("/hello")
    public String hello() {
        return "hello";
    }

    @GET
    @Path("/goodbye")
    public String goodbye() {
        return "goodbye";
    }

    @GET
    @Path("/goodmorning")
    public String goodmorning() {
        return "goodmorning";
    }

    @GET
    @Path("/goodevening")
    public String goodevening() {
        return "goodevening";
    }
}
