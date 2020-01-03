package io.quarkus.hibernate.validator.test.devmode;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

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
