package io.quarkus.it.jackson;

import java.io.IOException;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

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
