package io.quarkus.it.opentelemetry;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;

import io.quarkus.logging.Log;
import io.smallrye.context.api.CurrentThreadContext;
import io.smallrye.mutiny.Uni;
import io.smallrye.reactive.messaging.MutinyEmitter;
import io.vertx.core.Context;
import io.vertx.core.Vertx;

@Path("")
@Produces(MediaType.APPLICATION_JSON)
public class SimpleResource {

    @Channel("traces")
    Emitter<String> emitter;

    @Channel("traces-uni")
    MutinyEmitter<String> mutinyEmitter;

    @Inject
    RequestBean reqBean;

    @GET
    @Path("/direct")
    public TraceData directTrace() {
        TraceData data = new TraceData();
        data.message = "Direct trace";
        emitter.send(data.message).toCompletableFuture().join();

        return data;
    }

    @POST
    @Path("/uni")
    @Consumes(MediaType.TEXT_PLAIN)
    @CurrentThreadContext(propagated = {})
    public Uni<Void> uniEventLoop(String body) {
        Context ctx = Vertx.currentContext();
        Log.info(ctx.hashCode() + " " + ctx);
        Log.infof("bean: %s, id: %s", reqBean, reqBean.getId());
        reqBean.setName(body != null ? body.toUpperCase() : body);
        return mutinyEmitter.send(reqBean.getId());
    }

}
