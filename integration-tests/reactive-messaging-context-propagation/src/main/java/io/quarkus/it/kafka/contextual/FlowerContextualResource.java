package io.quarkus.it.kafka.contextual;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.reactive.messaging.Channel;

import io.quarkus.it.kafka.RequestBean;
import io.quarkus.logging.Log;
import io.quarkus.smallrye.reactivemessaging.runtime.ContextualEmitter;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Context;
import io.vertx.core.Vertx;

@Path("/flowers/contextual")
public class FlowerContextualResource {

    @Channel("contextual-flower")
    ContextualEmitter<String> emitter;

    @Channel("contextual-flower-blocking")
    ContextualEmitter<String> emitterBlocking;

    @Channel("contextual-flower-blocking-named")
    ContextualEmitter<String> emitterBlockingNamed;

    @Channel("contextual-flower-virtual-thread")
    ContextualEmitter<String> emitterVT;

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
