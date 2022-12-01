package io.quarkus.it.jsonb;

import java.io.IOException;

import javax.json.bind.Jsonb;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

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
