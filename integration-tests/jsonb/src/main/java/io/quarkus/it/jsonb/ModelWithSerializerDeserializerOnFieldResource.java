package io.quarkus.it.jsonb;

import java.io.IOException;

import jakarta.json.bind.Jsonb;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("fieldserder")
public class ModelWithSerializerDeserializerOnFieldResource {

    private final Jsonb jsonb;

    public ModelWithSerializerDeserializerOnFieldResource(Jsonb jsonb) {
        this.jsonb = jsonb;
    }

    @POST
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes(MediaType.APPLICATION_JSON)
    public String post(String body) throws IOException {
        ModelWithSerializerAndDeserializerOnField input = jsonb.fromJson(body,
                ModelWithSerializerAndDeserializerOnField.class);
        return input.getName() + "/" + input.getInner().getSomeValue();
    }

    @GET
    @Path("/{name}/{someValue}")
    @Produces(MediaType.APPLICATION_JSON)
    public String get(@PathParam("name") String name, @PathParam("someValue") String someValue) throws IOException {
        ModelWithSerializerAndDeserializerOnField input = new ModelWithSerializerAndDeserializerOnField(name,
                new ModelWithSerializerAndDeserializerOnField.Inner(someValue));
        return jsonb.toJson(input);
    }
}
