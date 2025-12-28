package io.quarkus.vertx.http;

import static io.quarkus.vertx.http.HttpServer.HTTP_SERVER;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import io.quarkus.registry.ValueRegistry;

@Singleton
public class HttpServerProducer {
    @Inject
    ValueRegistry valueRegistry;

    @Produces
    public HttpServer produces() {
        return valueRegistry.get(HTTP_SERVER);
    }
}
