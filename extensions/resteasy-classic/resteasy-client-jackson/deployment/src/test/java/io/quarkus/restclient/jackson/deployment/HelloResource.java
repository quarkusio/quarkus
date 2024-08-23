package io.quarkus.restclient.jackson.deployment;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Path("/hello")
public class HelloResource {

    @Inject
    ObjectMapper objectMapper;

    @GET
    public String hello() throws JsonProcessingException {
        return objectMapper
                .writeValueAsString(new DateDto(ZonedDateTime.of(1988, 11, 17, 0, 0, 0, 0, ZoneId.of("Europe/Paris"))));
    }
}
