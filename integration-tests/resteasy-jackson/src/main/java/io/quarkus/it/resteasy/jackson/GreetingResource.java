package io.quarkus.it.resteasy.jackson;

import java.sql.Date;
import java.time.LocalDate;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;

import org.jboss.resteasy.spi.ResteasyConfiguration;

@Path("/greeting")
public class GreetingResource {

    @GET
    public Greeting hello() {
        LocalDate localDate = LocalDate.of(2019, 01, 01);
        return new Greeting("hello", localDate, new Date(localDate.toEpochDay()));
    }

    @Path("config")
    @GET
    @Produces("text/plain")
    public String config(@Context ResteasyConfiguration config) {
        // test that configuration can be obtained from application.properties
        return config.getParameter("resteasy.gzip.max.input");
    }
}
