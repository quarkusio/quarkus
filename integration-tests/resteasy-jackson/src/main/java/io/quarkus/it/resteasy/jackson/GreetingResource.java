package io.quarkus.it.resteasy.jackson;

import java.time.LocalDate;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/")
public class GreetingResource {

    @GET
    @Path("/greeting")
    @Produces(MediaType.APPLICATION_JSON)
    public Greeting hello() {
        return new Greeting("hello", LocalDate.of(2019, 01, 01));
    }

    @GET
    @Path("/greeting2")
    @Produces(MediaType.APPLICATION_JSON)
    public Greeting hello2() {
        return new Greeting("hello2", LocalDate.of(2019, 01, 01));
    }
}
