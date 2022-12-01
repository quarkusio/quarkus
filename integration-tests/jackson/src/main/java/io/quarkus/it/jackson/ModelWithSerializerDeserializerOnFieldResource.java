package io.quarkus.it.jackson;

import java.io.IOException;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

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
