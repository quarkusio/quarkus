package io.quarkus.vertx.http.cdi;

import io.vertx.core.http.HttpServerOptions;

/**
 * Event class that is fired when Vert.x is now listening for HTTP / HTTPs requests
 */
public class VertxHttpListeningEvent {

    private final HttpServerOptions httpServerOptions;
    private final HttpServerOptions domainSocketOptions;
    private final HttpServerOptions httpsServerOptions;

    public VertxHttpListeningEvent(HttpServerOptions httpServerOptions,
            HttpServerOptions domainSocketOptions,
            HttpServerOptions httpsServerOptions) {
        this.httpServerOptions = httpServerOptions;
        this.domainSocketOptions = domainSocketOptions;
        this.httpsServerOptions = httpsServerOptions;
    }

    public HttpServerOptions getHttpServerOptions() {
        return httpServerOptions;
    }

    public HttpServerOptions getDomainSocketOptions() {
        return domainSocketOptions;
    }

    public HttpServerOptions getHttpsServerOptions() {
        return httpsServerOptions;
    }
}
