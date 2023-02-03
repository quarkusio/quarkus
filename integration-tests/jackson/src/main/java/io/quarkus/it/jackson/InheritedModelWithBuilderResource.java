package io.quarkus.it.jackson;

import java.io.IOException;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import io.quarkus.it.jackson.model.InheritedModelWithBuilder;

@Path("/inheritedmodelwithbuilder")
public class InheritedModelWithBuilderResource {

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response newModel(String body) throws IOException {
        InheritedModelWithBuilder model = InheritedModelWithBuilder.fromJson(body);
        return Response.status(201).entity(model.toJson()).build();
    }
}
