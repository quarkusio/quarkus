package io.quarkus.it.jackson;

import java.io.IOException;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.it.jackson.model.ModelWithJsonTypeIdResolver;

@Path("/typeIdResolver")
public class ModelWithJsonTypeIdResolverResource {

    private final ObjectMapper objectMapper;

    public ModelWithJsonTypeIdResolverResource(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @POST
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes(MediaType.APPLICATION_JSON)
    public String post(String body) throws IOException {
        ModelWithJsonTypeIdResolver input = objectMapper.readValue(body, ModelWithJsonTypeIdResolver.class);
        return input.getType();
    }

    @GET
    @Path("one")
    @Produces(MediaType.APPLICATION_JSON)
    public String one() throws IOException {
        return objectMapper.writeValueAsString(new ModelWithJsonTypeIdResolver.SubclassOne());
    }

    @GET
    @Path("two")
    @Produces(MediaType.APPLICATION_JSON)
    public String two() throws IOException {
        return objectMapper.writeValueAsString(new ModelWithJsonTypeIdResolver.SubclassTwo());
    }
}
