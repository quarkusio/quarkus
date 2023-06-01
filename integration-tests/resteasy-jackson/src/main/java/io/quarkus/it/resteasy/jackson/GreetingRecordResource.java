package io.quarkus.it.resteasy.jackson;

import java.sql.Date;
import java.time.LocalDate;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/greetingRecord")
public class GreetingRecordResource {

    @GET
    public GreetingRecord hello() {
        LocalDate localDate = LocalDate.of(2019, 01, 01);
        return new GreetingRecord("hello", localDate, new Date(localDate.toEpochDay()));
    }
}
