package io.quarkus.it.opentelemetry;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;

@Path("")
@Produces(MediaType.APPLICATION_JSON)
public class SimpleResource {

    @Channel("traces")
    Emitter<String> emitter;

    @GET
    @Path("/direct")
    public TraceData directTrace() {
        TraceData data = new TraceData();
        data.message = "Direct trace";
        emitter.send(data.message).toCompletableFuture().join();

        return data;
    }

}
