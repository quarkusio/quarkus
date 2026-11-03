package io.quarkus.it.tls.letsencrypt;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import io.vertx.ext.web.Router;

/**
 * A route that the tests call over HTTPS.
 */
@ApplicationScoped
public class HelloRoute {

    /**
     * Register the route.
     *
     * @param router the router of the HTTP server
     */
    public void register(@Observes Router router) {
        router.get("/hello").handler(rc -> rc.response().end("hello"));
    }
}
