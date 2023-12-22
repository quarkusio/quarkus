package io.quarkus.jfr.it;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.quarkus.jfr.runtime.RequestIdProducer;

@Path("")
@ApplicationScoped
public class RequestIdResource {

    @Inject
    RequestIdProducer idProducer;

    @Path("/requestId")
    @Produces(MediaType.APPLICATION_JSON)
    @GET
    public Id hello() {
        return new Id(idProducer.create().id);
    }

    class Id {

        public String id;

        public Id() {
        }

        Id(String id) {
            this.id = id;
        }
    }
}
