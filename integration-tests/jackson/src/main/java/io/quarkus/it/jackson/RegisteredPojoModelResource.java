package io.quarkus.it.jackson;

import java.io.IOException;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import io.quarkus.it.jackson.model.RegisteredPojoModel;

@Path("/registeredpojomodel")
public class RegisteredPojoModelResource {

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response newModel(String body) throws IOException {
        RegisteredPojoModel model = RegisteredPojoModel.fromJson(body);
        return Response.status(201).entity(model.toJson()).build();
    }
}
