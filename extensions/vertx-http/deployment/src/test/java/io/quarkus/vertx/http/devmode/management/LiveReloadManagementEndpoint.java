package io.quarkus.vertx.http.devmode.management;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import io.quarkus.vertx.http.ManagementInterface;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.WebClient;

// Case 3: import jakarta.inject.Inject;

@ApplicationScoped
public class LiveReloadManagementEndpoint {

    // Case 3: @Inject
    // Case 3: LiveReloadManagementHandlerAsCDIBean handler;

    void managementRoutes(@Observes ManagementInterface mi) {
        Router router = mi.router();
        // Case 1: testing name change:
        router.route("/manage-1")
                .produces("text/plain")
                .handler(rc -> rc.response().end(WebClient.class.hashCode() + "-" + HttpRequest.class.hashCode()));

        // Test that a new handler using some CDI bean can be loaded:
        // Case 2: router.route("/manage-cdi")
        // Case 2:         .produces("text/plain")
        // Case 2:         .handler(new LiveReloadManagementHandler());

        // Case 3: router.route("/manage-bean-handler")
        // Case 3:         .produces("text/plain")
        // Case 3:         .handler(handler);
    }
}
