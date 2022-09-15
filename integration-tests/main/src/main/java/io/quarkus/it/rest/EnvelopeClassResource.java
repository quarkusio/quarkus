package io.quarkus.it.rest;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

@Path("/envelope")
public class EnvelopeClassResource {

    @Path("/payload")
    @GET
    @Produces("application/json")
    public EnvelopeClass<PayloadClass> payloadClass() {
        return new EnvelopeClass<>(new PayloadClass("hello"));
    }
}
