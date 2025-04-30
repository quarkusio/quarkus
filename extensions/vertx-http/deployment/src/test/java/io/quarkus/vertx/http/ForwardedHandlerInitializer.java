package io.quarkus.vertx.http;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.Router;

@ApplicationScoped
public class ForwardedHandlerInitializer {

    public void register(@Observes Router router) {
        router.route("/forward").handler(rc -> rc.response()
                .end(rc.request().scheme() + "|" + rc.request().getHeader(HttpHeaders.HOST) + "|"
                        + rc.request().remoteAddress().toString()));
        router.route("/path").handler(rc -> rc.response()
                .end(rc.request().scheme()
                        + "|" + rc.request().getHeader(HttpHeaders.HOST)
                        + "|" + rc.request().remoteAddress().toString()
                        + "|" + rc.request().uri()
                        + "|" + rc.request().absoluteURI()));
        router.route("/trusted-proxy").handler(rc -> rc.response()
                .end(rc.request().scheme() + "|" + rc.request().getHeader(HttpHeaders.HOST) + "|"
                        + rc.request().remoteAddress().toString()
                        + "|" + rc.request().getHeader("X-Forwarded-Trusted-Proxy")));
        router.route("/path-trusted-proxy").handler(rc -> rc.response()
                .end(rc.request().scheme()
                        + "|" + rc.request().getHeader(HttpHeaders.HOST)
                        + "|" + rc.request().remoteAddress().toString()
                        + "|" + rc.request().uri()
                        + "|" + rc.request().absoluteURI()
                        + "|" + rc.request().getHeader("X-Forwarded-Trusted-Proxy")));
    }

}
