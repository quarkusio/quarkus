package io.quarkus.it.webjar.locator;

import java.time.LocalDate;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/greeting")
public class GreetingResource {

    @GET
    public Greeting hello() {
        return new Greeting("hello", LocalDate.of(2019, 01, 01));
    }
}
