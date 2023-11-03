package io.quarkus.it.opentelemetry.util;

import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

@Path("/otel/enduser")
@RequestScoped
public class EndUserResource {

    @GET
    public Response dummy() {
        return Response.ok().build();
    }

}
