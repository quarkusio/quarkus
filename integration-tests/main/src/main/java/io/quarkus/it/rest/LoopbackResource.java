package io.quarkus.it.rest;

import java.time.LocalDate;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/loopback")
public class LoopbackResource {

    @GET
    public Greeting hello() {
        return new Greeting("hello self", LocalDate.of(2020, 02, 13));
    }

}
