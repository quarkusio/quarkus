package io.quarkus.it.hibernate.validator;

import javax.validation.constraints.Digits;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

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
