package io.quarkus.it.resteasy.jsonb;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

@Path("/greeter")
public class GreeterResource {

    @GET
    @Produces("application/json")
    public Greeter greeting() {
        return new Greeting("hello");
    }
}
