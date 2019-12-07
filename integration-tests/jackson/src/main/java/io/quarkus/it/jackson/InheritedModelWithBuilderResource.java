package io.quarkus.it.jackson;

import java.io.IOException;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

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
