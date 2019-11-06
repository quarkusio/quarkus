package io.quarkus.resteasy.runtime.standalone;

import java.util.Objects;

import org.jboss.logging.Logger;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.net.SocketAddress;

final class LazyHostSupplier {
    private static final Logger logger = Logger.getLogger(LazyHostSupplier.class.getName());

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
            SocketAddress socketAddress = null;
            try {
                socketAddress = request.remoteAddress();
            } catch (NullPointerException npe) {
                // ideally there shouldn't be an exception for this call.
                // This is just to workaround a current issue in vertx
                // https://github.com/quarkusio/quarkus/issues/5247
                // https://github.com/eclipse-vertx/vert.x/issues/3181
                // TODO: remove this workaround once vertx issue is resolved
                logger.debug("Ignoring exception that occurred when obtaining remote address of request " + request, npe);
            }
            // client address may not be available with VirtualHttp;
            this.remoteHost = socketAddress != null ? socketAddress.host() : null;
            initialized = true;
            return this.remoteHost;
        }
    }
}
