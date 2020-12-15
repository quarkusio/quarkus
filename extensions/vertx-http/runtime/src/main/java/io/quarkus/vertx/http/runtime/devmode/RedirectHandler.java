package io.quarkus.vertx.http.runtime.devmode;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

public class RedirectHandler implements Handler<RoutingContext> {
    @Override
    public void handle(RoutingContext event) {
        event.response().setStatusCode(HttpResponseStatus.TEMPORARY_REDIRECT.code())
                .putHeader(HttpHeaderNames.LOCATION, event.request().absoluteURI() + "/").end();
    }
}
