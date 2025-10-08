package io.quarkus.it.jackson;

import java.util.List;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import io.quarkus.it.jackson.model.Elephant;
import io.quarkus.it.jackson.model.MammalFamily;
import io.quarkus.it.jackson.model.Whale;

@Path("jsonSubTypes")
public class JsonSubTypesResource {

    private final ObjectMapper objectMapper;

    public JsonSubTypesResource(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @GET
    public String test() throws JacksonException {
        return objectMapper.writeValueAsString(new MammalFamily(List.of(
                new Whale(30.0, "white"), new Elephant(10, "africa"))));
    }
}
