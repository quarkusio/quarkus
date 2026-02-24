package io.quarkus.it.rest.client.main;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import io.quarkus.vertx.web.Route;
import io.smallrye.common.annotation.Blocking;
import io.vertx.ext.web.RoutingContext;

@ApplicationScoped
public class RedirectResource {
    @RestClient
    RedirectResourceClient redirectResourceClient;

    @ConfigProperty(name = "test.url")
    String testUrl;

    @Blocking
    @Route(path = "/redirect", methods = Route.HttpMethod.GET)
    String execute() {
        return redirectResourceClient.redirectResponse();
    }

    @Route(path = "/redirect/response", methods = Route.HttpMethod.POST)
    void redirectResponse(RoutingContext rc) {
        rc.response()
                .putHeader("Location", "%s/redirect/other".formatted(testUrl))
                .setStatusCode(302)
                .end();
    }

    @Blocking
    @Route(path = "/redirect/other", methods = Route.HttpMethod.GET)
    String other() {
        return "other";
    }
}
