package io.quarkus.it.resteasy.reactive;

import java.sql.Date;
import java.time.LocalDate;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/greeting")
public class GreetingResource {

    @GET
    public Greeting hello() {
        LocalDate localDate = LocalDate.of(2019, 01, 01);
        return new Greeting("hello", localDate, new Date(localDate.toEpochDay()));
    }

}
