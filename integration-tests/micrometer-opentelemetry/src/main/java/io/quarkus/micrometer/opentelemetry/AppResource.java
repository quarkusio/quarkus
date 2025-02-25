package io.quarkus.micrometer.opentelemetry;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import io.quarkus.micrometer.opentelemetry.services.CountedBean;

@Path("")
@Produces(MediaType.APPLICATION_JSON)
public class AppResource {

    @Inject
    CountedBean countedBean;

    @Path("/count")
    @GET
    public Response count(@QueryParam("fail") boolean fail) {
        countedBean.countAllInvocations(fail);
        return Response.ok().build();
    }
}
