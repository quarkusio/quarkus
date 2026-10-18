package io.quarkus.it.pulsar;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/pulsar")
public class PulsarEndpoint {
    @Inject
    PulsarReceivers receivers;

    @GET
    @Path("/fruits")
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> getFruits() {
        return receivers.getFruits();
    }

}
