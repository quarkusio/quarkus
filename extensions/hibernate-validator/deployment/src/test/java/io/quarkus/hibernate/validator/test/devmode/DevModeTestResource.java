package io.quarkus.hibernate.validator.test.devmode;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/test")
public class DevModeTestResource {

    @Inject
    DependentTestBean bean;

    @POST
    @Path("/validate")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public String validateBean(@Valid TestBean testBean) {
        return "ok";
    }

    @GET
    @Path("/{message}")
    @Produces(MediaType.TEXT_PLAIN)
    public String validateCDIBean(@PathParam("message") String message) {
        return bean.testMethod(message);
    }
}
