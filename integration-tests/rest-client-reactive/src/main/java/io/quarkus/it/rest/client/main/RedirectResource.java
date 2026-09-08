package io.quarkus.it.rest.client.main;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import io.quarkus.vertx.http.HttpServer;
import io.quarkus.vertx.web.Route;
import io.smallrye.common.annotation.Blocking;
import io.vertx.ext.web.RoutingContext;

@ApplicationScoped
public class RedirectResource {
    @Inject
    HttpServer httpServer;
    @RestClient
    RedirectResourceClient redirectResourceClient;

    @Blocking
    @Route(path = "/redirect", methods = Route.HttpMethod.GET)
    String execute() {
        return redirectResourceClient.redirectResponse();
    }

    @Route(path = "/redirect/response", methods = Route.HttpMethod.POST)
    void redirectResponse(RoutingContext rc) {
        rc.response()
                .putHeader("Location", "%s/redirect/other".formatted(httpServer.getLocalBaseUri()))
                .setStatusCode(302)
                .end();
    }

    @Blocking
    @Route(path = "/redirect/other", methods = Route.HttpMethod.GET)
    String other() {
        return "other";
    }
}
