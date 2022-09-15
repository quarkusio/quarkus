package io.quarkus.it.vertx;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.Router;

@ApplicationScoped
public class BeanRegisteringRoute {

    protected static final String PING_DATA = "12345678";

    void init(@Observes Router router) {
        router.route("/my-path").handler(rc -> rc.response().end("OK"));

        //ping only works on HTTP/2
        router.get("/ping").handler(rc -> {
            rc.request().connection().ping(Buffer.buffer(PING_DATA), new Handler<AsyncResult<Buffer>>() {
                @Override
                public void handle(AsyncResult<Buffer> event) {
                    rc.response().end(event.result());
                }
            });
        });
    }
}
