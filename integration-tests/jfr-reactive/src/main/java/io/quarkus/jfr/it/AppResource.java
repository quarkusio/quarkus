package io.quarkus.jfr.it;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import io.quarkus.jfr.runtime.IdProducer;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

@Path("/app")
@ApplicationScoped
public class AppResource {

    @Inject
    IdProducer idProducer;

    @Inject
    RoutingContext routingContext;

    @GET
    @Path("/reactive")
    public Uni<IdResponse> reactive() {
        return Uni.createFrom().item(new IdResponse(idProducer.getTraceId(), idProducer.getSpanId()));
    }

    @GET
    @Path("blocking")
    public IdResponse blocking() {
        return new IdResponse(idProducer.getTraceId(), idProducer.getSpanId());
    }

    @GET
    @Path("error")
    public void error() {
        throw new JfrTestException(idProducer.getTraceId());
    }
}
