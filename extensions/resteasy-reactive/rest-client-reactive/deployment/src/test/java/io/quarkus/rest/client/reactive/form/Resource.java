package io.quarkus.rest.client.reactive.form;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.reactive.RestForm;

@Path("")
@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
@Produces(MediaType.TEXT_PLAIN)
public class Resource {

    @POST
    public Response getUriEntityAndQueryParam(@RestForm String formParam1,
            @RestForm String formParam2) {
        return Response.ok(String.format("root formParam1:%s,formParam2:%s", formParam1, formParam2)).build();
    }

    @PUT
    @Path("/sub")
    public Response getUriEntityAndQueryParam(@RestForm String rootParam1,
            @RestForm String rootParam2,
            @RestForm String subParam1,
            @RestForm String subParam2) {
        return Response.ok(String.format("sub rootParam1:%s,rootParam2:%s,subParam1:%s,subParam2:%s",
                rootParam1, rootParam2, subParam1, subParam2)).build();
    }

    @POST
    @Path("/types")
    public Response getWithTypes(@RestForm String text, @RestForm int number, @RestForm Integer wrapNumber,
            @RestForm Mode mode) {
        return Response.ok(String.format("root text:%s,number:%s,wrapNumber:%s,mode:%s", text, number, wrapNumber, mode))
                .build();
    }
}
