package io.quarkus.http3.runtime;

import org.jboss.logging.Logger;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpVersion;
import io.vertx.ext.web.RoutingContext;

/**
 * This handler advertises the availability of HTTP/3 when HTTP/1 or HTTP/2 is used.
 * HTTP/1 uses a response header. HTTP/2 uses a custom frame.
 */
public class Http3AltSvcHandler implements Handler<RoutingContext> {

    private static final Logger LOG = Logger.getLogger(Http3AltSvcHandler.class);

    @Override
    public void handle(RoutingContext rc) {
        if (rc.request().isSSL() && rc.request().version() != HttpVersion.HTTP_3) {
            int port = rc.request().localAddress().port();
            String advertisement = "h3=\":" + port + "\"; ma=86400"; // Allows caching the advertising for 1 day.
            rc.response().writeAltSvc(advertisement)
                    .onFailure(t -> LOG.warn("Failed to write Alt-Svc advertisement", t));
        }
        rc.next();
    }
}
