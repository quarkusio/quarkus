package io.quarkus.it.hibernate.validator;

import javax.validation.constraints.Digits;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

public interface HibernateValidatorTestResourceInterface {

    @GET
    @Path("/rest-end-point-interface-validation/{id}/")
    @Produces(MediaType.TEXT_PLAIN)
    public String testRestEndPointInterfaceValidation(@Digits(integer = 5, fraction = 0) @PathParam("id") String id);

    @GET
    @Path("/rest-end-point-interface-validation-annotation-on-impl-method/{id}/")
    @Produces(MediaType.TEXT_PLAIN)
    public String testRestEndPointInterfaceValidationWithAnnotationOnImplMethod(
            @Digits(integer = 5, fraction = 0) @PathParam("id") String id);
}
