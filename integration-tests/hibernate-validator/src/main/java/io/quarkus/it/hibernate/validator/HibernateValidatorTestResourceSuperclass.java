package io.quarkus.it.hibernate.validator;

import jakarta.validation.constraints.Digits;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

public class HibernateValidatorTestResourceSuperclass {

    @GET
    @Path("/rest-end-point-interface-validation-annotation-on-overridden-method/{id}/")
    @Produces(MediaType.TEXT_PLAIN)
    public String testRestEndPointInterfaceValidationWithAnnotationOnOverriddenMethod(
            @Digits(integer = 5, fraction = 0) @PathParam("id") String id) {
        return id;
    }
}
