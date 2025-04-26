package org.acme;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.acme.lib.StringTypeProtectedConstructor;
import org.acme.lib.GreetingListFilter;

@Path("/hello")
public class ParametersResource {

    @Path("{stringValue}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String stringTypePath(@PathParam("stringValue") final StringTypeProtectedConstructor stringValue) {
        return stringValue.getValue();
    }

    @Path("parameterized-type-external")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello(@BeanParam GreetingListFilter<GreetingListSortAttribute> listFilter) {
        return "OK";
    }
}
