package io.quarkus.it.resteasy.jackson;

import java.sql.Date;
import java.time.LocalDate;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/greeting")
public class GreetingResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Greeting hello() {
        LocalDate localDate = LocalDate.of(2019, 01, 01);
        return new Greeting("hello", localDate, new Date(localDate.toEpochDay()));
    }
}
