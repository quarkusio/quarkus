package io.quarkus.it.kafka.contextual;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.reactive.messaging.Channel;

import io.quarkus.it.kafka.RequestBean;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import io.smallrye.reactive.messaging.MutinyEmitter;
import io.vertx.core.Context;
import io.vertx.core.Vertx;

@Path("/flowers/mutiny")
public class FlowerMutinyResource {

    @Channel("mutiny-flower")
    MutinyEmitter<String> emitter;

    @Channel("mutiny-flower-blocking")
    MutinyEmitter<String> emitterBlocking;

    @Channel("mutiny-flower-blocking-named")
    MutinyEmitter<String> emitterBlockingNamed;

    @Channel("mutiny-flower-virtual-thread")
    MutinyEmitter<String> emitterVT;

    @Inject
    RequestBean reqBean;

    @POST
    @Path("/uni")
    @Consumes(MediaType.TEXT_PLAIN)
    public Uni<Void> uniEventLoop(String body) {
        Context ctx = Vertx.currentContext();
        Log.info(ctx + "[" + ctx.getClass() + "]");
        Log.infof("bean: %s, id: %s", reqBean, reqBean.getId());
        reqBean.setName(body != null ? body.toUpperCase() : body);
        return emitter.send(reqBean.getId());
    }

    @POST
    @Path("/uni/blocking")
    @Consumes(MediaType.TEXT_PLAIN)
    public Uni<Void> uniBlocking(String body) {
        Context ctx = Vertx.currentContext();
        Log.info(ctx + "[" + ctx.getClass() + "]");
        Log.infof("bean: %s, id: %s", reqBean, reqBean.getId());
        reqBean.setName(body != null ? body.toUpperCase() : body);
        return emitterBlocking.send(reqBean.getId());
    }

    @POST
    @Path("/uni/blocking-named")
    @Consumes(MediaType.TEXT_PLAIN)
    public Uni<Void> uniBlockingNamed(String body) {
        Context ctx = Vertx.currentContext();
        Log.info(ctx + "[" + ctx.getClass() + "]");
        Log.infof("bean: %s, id: %s", reqBean, reqBean.getId());
        reqBean.setName(body != null ? body.toUpperCase() : body);
        return emitterBlockingNamed.send(reqBean.getId());
    }

    @POST
    @Path("/uni/virtual-thread")
    @Consumes(MediaType.TEXT_PLAIN)
    public Uni<Void> uniVT(String body) {
        Context ctx = Vertx.currentContext();
        Log.info(ctx + "[" + ctx.getClass() + "]");
        Log.infof("bean: %s, id: %s", reqBean, reqBean.getId());
        reqBean.setName(body != null ? body.toUpperCase() : body);
        return emitterVT.send(reqBean.getId());
    }

    @POST
    @Path("/")
    @Consumes(MediaType.TEXT_PLAIN)
    public void eventLoop(String body) {
        Context ctx = Vertx.currentContext();
        Log.info(ctx + "[" + ctx.getClass() + "]");
        Log.infof("bean: %s, id: %s", reqBean, reqBean.getId());
        reqBean.setName(body != null ? body.toUpperCase() : body);
        emitter.sendAndAwait(reqBean.getId());
    }

    @POST
    @Path("/blocking")
    @Consumes(MediaType.TEXT_PLAIN)
    public void blocking(String body) {
        Context ctx = Vertx.currentContext();
        Log.info(ctx + "[" + ctx.getClass() + "]");
        Log.infof("bean: %s, id: %s", reqBean, reqBean.getId());
        reqBean.setName(body != null ? body.toUpperCase() : body);
        emitterBlocking.sendAndAwait(reqBean.getId());
    }

    @POST
    @Path("/blocking-named")
    @Consumes(MediaType.TEXT_PLAIN)
    public void blockingNamed(String body) {
        Context ctx = Vertx.currentContext();
        Log.info(ctx + "[" + ctx.getClass() + "]");
        Log.infof("bean: %s, id: %s", reqBean, reqBean.getId());
        reqBean.setName(body != null ? body.toUpperCase() : body);
        emitterBlockingNamed.sendAndAwait(reqBean.getId());
    }

    @POST
    @Path("/virtual-thread")
    @Consumes(MediaType.TEXT_PLAIN)
    public void vt(String body) {
        Context ctx = Vertx.currentContext();
        Log.info(ctx + "[" + ctx.getClass() + "]");
        Log.infof("bean: %s, id: %s", reqBean, reqBean.getId());
        reqBean.setName(body != null ? body.toUpperCase() : body);
        emitterVT.sendAndAwait(reqBean.getId());
    }
}
