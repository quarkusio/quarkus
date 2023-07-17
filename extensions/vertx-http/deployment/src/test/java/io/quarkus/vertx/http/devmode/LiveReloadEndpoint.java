package io.quarkus.vertx.http.devmode;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import io.quarkus.vertx.http.runtime.HttpBuildTimeConfig;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.WebClient;

@Named
@ApplicationScoped
public class LiveReloadEndpoint {

    @Inject
    HttpBuildTimeConfig httpConfig;

    void addConfigRoute(@Observes Router router) {
        router.route("/test")
                .produces("text/plain")
                .handler(rc -> rc.response().end(WebClient.class.hashCode() + "-" + HttpRequest.class.hashCode()));
    }

}
