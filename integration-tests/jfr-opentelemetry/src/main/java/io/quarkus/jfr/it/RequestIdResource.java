package io.quarkus.jfr.it;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.quarkus.jfr.runtime.IdProducer;

@Path("")
@ApplicationScoped
public class RequestIdResource {

    @Inject
    IdProducer idProducer;

    @Path("/requestId")
    @Produces(MediaType.APPLICATION_JSON)
    @GET
    public IdResponse hello() {
        return new IdResponse(idProducer.getTraceId(), idProducer.getSpanId());
    }
}
