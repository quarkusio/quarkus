package io.quarkus.it.jackson;

import java.io.IOException;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

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
