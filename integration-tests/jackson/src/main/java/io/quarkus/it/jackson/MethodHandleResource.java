package io.quarkus.it.jackson;

import java.io.IOException;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import tools.jackson.databind.ObjectMapper;

@Path("/method-handle")
public class MethodHandleResource {

    private final ObjectMapper objectMapper;

    public MethodHandleResource(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String reflect(String json) throws IOException {
        MethodHandlePojo pojo = objectMapper.readValue(json, MethodHandlePojo.class);
        return objectMapper.writeValueAsString(pojo);
    }
}
