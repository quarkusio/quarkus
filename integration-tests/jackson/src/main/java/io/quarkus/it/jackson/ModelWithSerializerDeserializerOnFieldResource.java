package io.quarkus.it.jackson;

import java.io.IOException;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.it.jackson.model.ModelWithSerializerAndDeserializerOnField;

@Path("fieldserder")
public class ModelWithSerializerDeserializerOnFieldResource {

    private final ObjectMapper objectMapper;

    public ModelWithSerializerDeserializerOnFieldResource(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @POST
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes(MediaType.APPLICATION_JSON)
    public String post(String body) throws IOException {
        ModelWithSerializerAndDeserializerOnField input = objectMapper.readValue(body,
                ModelWithSerializerAndDeserializerOnField.class);
        return input.getName() + "/" + input.getInner().getSomeValue();
    }

    @GET
    @Path("/{name}/{someValue}")
    @Produces(MediaType.APPLICATION_JSON)
    public String get(@PathParam("name") String name, @PathParam("someValue") String someValue) throws IOException {
        ModelWithSerializerAndDeserializerOnField input = new ModelWithSerializerAndDeserializerOnField(name,
                new ModelWithSerializerAndDeserializerOnField.Inner(someValue));
        return objectMapper.writeValueAsString(input);
    }
}
