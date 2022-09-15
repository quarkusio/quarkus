package io.quarkus.it.jackson;

import java.io.IOException;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.it.jackson.model.ModelWithJsonDeserializeUsing;

@Path("/deserializerUsing")
public class ModelWithJsonDeserializeUsingResource {

    private final ObjectMapper objectMapper;

    public ModelWithJsonDeserializeUsingResource(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @POST
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes(MediaType.APPLICATION_JSON)
    public String post(String body) throws IOException {
        ModelWithJsonDeserializeUsing input = objectMapper.readValue(body,
                ModelWithJsonDeserializeUsing.class);
        return input.getSomeValue();
    }
}
