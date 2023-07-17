package io.quarkus.resteasy.jackson;

import java.time.ZonedDateTime;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Produces(MediaType.APPLICATION_JSON)
@Path("/hello")
public class HelloResource {

    @GET
    public DateDto hello() {
        return new DateDto(ZonedDateTime.now());
    }
}
