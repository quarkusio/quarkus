package io.quarkus.resteasy.jackson;

import java.time.ZonedDateTime;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Produces(MediaType.APPLICATION_JSON)
@Path("/hello")
public class HelloResource {

    @GET
    public DateDto hello() {
        return new DateDto(ZonedDateTime.now());
    }
}
