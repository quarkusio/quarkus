package io.quarkus.restclient.jsonb.deployment;

import java.time.ZonedDateTime;

import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

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
