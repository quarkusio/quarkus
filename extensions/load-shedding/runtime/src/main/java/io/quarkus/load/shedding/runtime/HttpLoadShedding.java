package io.quarkus.load.shedding.runtime;

import jakarta.annotation.Priority;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Singleton;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.Router;

@Singleton
public class HttpLoadShedding {
    public void init(@Observes @Priority(-1_000_000_000) Router router, OverloadDetector detector,
            PriorityLoadShedding priority, LoadSheddingRuntimeConfig config) {

        if (!config.enabled()) {
            return;
        }

        router.route().order(-1_000_000_000).handler(ctx -> {
            boolean accepted = detector.tryBeginRequest();
            if (!accepted && priority.shedLoad(ctx)) {
                HttpServerResponse response = ctx.response();
                response.setStatusCode(HttpResponseStatus.SERVICE_UNAVAILABLE.code());
                response.headers().add(HttpHeaderNames.CONNECTION, "close");
                response.endHandler(new Handler<Void>() {
                    @Override
                    public void handle(Void ignored) {
                        ctx.request().connection().close();
                    }
                });
                response.end();
            } else {
                // If not accepted but priority says don't shed, force increment for high-priority request
                if (!accepted) {
                    detector.requestBegin();
                }
                long start = System.nanoTime();
                ctx.addEndHandler(new Handler<AsyncResult<Void>>() {
                    @Override
                    public void handle(AsyncResult<Void> ignored) {
                        long end = System.nanoTime();
                        detector.requestEnd((end - start) / 1_000);
                    }
                });
                ctx.next();
            }
        });
    }
}
