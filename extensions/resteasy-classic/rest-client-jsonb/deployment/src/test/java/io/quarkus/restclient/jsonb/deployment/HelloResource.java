package io.quarkus.restclient.jsonb.deployment;

import java.time.ZonedDateTime;

import jakarta.inject.Inject;
import jakarta.json.bind.Jsonb;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/hello")
public class HelloResource {

    @Inject
    Jsonb jsonb;

    @GET
    public String hello() {
        // we don't care about the value here as we will use a custom deserializer that returns a fixed value
        return jsonb.toJson(new DateDto(ZonedDateTime.now()));
    }
}
