package io.quarkus.it.jackson;

import java.io.IOException;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

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
        return objectMapper.writeValueAsString(new SampleResponse("My blog post", "best"));
    }
}
