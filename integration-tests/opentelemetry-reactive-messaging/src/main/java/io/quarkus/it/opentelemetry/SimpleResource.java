package io.quarkus.it.opentelemetry;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

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
