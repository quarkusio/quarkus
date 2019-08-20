package io.quarkus.it.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

@Path("/envelope")
public class EnvelopeClassResource {

    @Path("/payload")
    @GET
    @Produces("application/json")
    public EnvelopeClass<PayloadClass> payloadClass() {
        return new EnvelopeClass<>(new PayloadClass("hello"));
    }
}
