package io.quarkus.jfr.it;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.quarkus.jfr.runtime.RequestIdProducer;
import io.quarkus.jfr.runtime.TracingRequestId;
import io.quarkus.jfr.runtime.TracingRequestIdProducer;

@Path("")
@ApplicationScoped
public class RequestIdResource {

    @Inject
    RequestIdProducer idProducer;

    @Path("/requestId")
    @Produces(MediaType.APPLICATION_JSON)
    @GET
    public TracingId hello() {
        TracingRequestIdProducer tracingRequestIdProducer = (TracingRequestIdProducer) idProducer;
        TracingRequestId id = tracingRequestIdProducer.create();
        return new TracingId(id.id, id.traceId, id.spanId);
    }

    class TracingId {

        public String id;
        public String traceId;
        public String spanId;

        public TracingId() {
        }

        public TracingId(String id, String traceId, String spanId) {
            this.id = id;
            this.traceId = traceId;
            this.spanId = spanId;
        }
    }
}
