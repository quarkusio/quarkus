package io.quarkus.vertx.http.runtime;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

@Singleton
public class HttpServerProducer {
    @Produces
    @Singleton
    public HttpServer producer() {
        return HttpServer.instance();
    }
}
