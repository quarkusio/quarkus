package io.quarkus.it.jackson;

import java.io.IOException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.it.jackson.model.SampleResponse;

@Path("/json-naming/")
public class ModelWithJsonNamingStrategyResource {

    private final ObjectMapper objectMapper;

    public ModelWithJsonNamingStrategyResource(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String get() throws IOException {
        return objectMapper.writeValueAsString(new SampleResponse("My blog post"));
    }
}
