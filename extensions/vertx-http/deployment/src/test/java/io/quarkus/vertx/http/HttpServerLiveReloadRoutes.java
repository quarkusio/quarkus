package io.quarkus.vertx.http;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import io.quarkus.runtime.AddressResolver;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.WebClient;

@ApplicationScoped
public class HttpServerLiveReloadRoutes {

    @Inject
    HttpServer httpServer;

    void routes(@Observes Router router) {
        // The application installs its router before it receives any request.
        int portAtRouterSetup = httpServer.getPort();
        router.get("/port").handler(rc -> rc.end(String.valueOf(httpServer.getPort())));
        router.get("/port-at-router-setup").handler(rc -> rc.end(String.valueOf(portAtRouterSetup)));
        router.get("/hello").handler(rc -> rc.end("hello"));
        router.get("/call-myself").handler(rc -> {
            WebClient client = WebClient.create(rc.vertx());
            client.getAbs(AddressResolver.resolve("quarkus://vertx-http/hello").toString()).send()
                    .onComplete(ignored -> client.close())
                    .onSuccess(response -> rc.end(response.bodyAsString()))
                    .onFailure(rc::fail);
        });
    }
}
