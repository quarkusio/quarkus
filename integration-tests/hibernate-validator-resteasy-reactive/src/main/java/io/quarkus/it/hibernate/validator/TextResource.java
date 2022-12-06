package io.quarkus.it.hibernate.validator;

import jakarta.validation.constraints.Digits;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("text")
public class TextResource {

    @GET
    @Path("/validate/{id}")
    public String validate(
            @Digits(integer = 5, fraction = 0, message = "numeric value out of bounds") @PathParam("id") String id) {
        return id;
    }

    @GET
    @Path("/validate/text/{id}")
    @Produces(MediaType.TEXT_PLAIN)
    public String validateText(
            @Digits(integer = 5, fraction = 0, message = "numeric value out of bounds") @PathParam("id") String id) {
        return id;
    }
}
