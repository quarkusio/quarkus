package io.quarkus.resteasy.runtime.standalone;

import java.util.Objects;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.net.SocketAddress;

final class LazyHostSupplier {

    private final HttpServerRequest request;
    private String remoteHost = null;
    private boolean initialized = false;

    public LazyHostSupplier(final HttpServerRequest request) {
        Objects.requireNonNull(request);
        this.request = request;
    }

    public String getRemoteHost() {
        if (initialized) {
            return this.remoteHost;
        } else {
            SocketAddress socketAddress = request.remoteAddress();
            // client address may not be available with VirtualHttp;
            this.remoteHost = socketAddress != null ? socketAddress.host() : null;
            initialized = true;
            return this.remoteHost;
        }
    }
}
