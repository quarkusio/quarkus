package io.quarkus.vertx.http;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

import io.vertx.ext.web.Router;

@ApplicationScoped
class ForwardedHandlerInitializer {

    public void register(@Observes Router router) {
        router.route("/forward").handler(rc -> rc.response()
                .end(rc.request().scheme() + "|" + rc.request().host() + "|" + rc.request().remoteAddress().toString()));
    }

}
