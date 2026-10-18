package io.quarkus.http3.runtime;

import java.time.Duration;

import org.eclipse.microprofile.config.ConfigProvider;
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

    private final long maxAgeSeconds;

    public Http3AltSvcHandler() {
        this.maxAgeSeconds = ConfigProvider.getConfig()
                .getOptionalValue("quarkus.http3.alt-svc-max-age", Duration.class).map(Duration::getSeconds).orElse(86400L);
    }

    @Override
    public void handle(RoutingContext rc) {
        if (rc.request().isSSL() && rc.request().version() != HttpVersion.HTTP_3) {
            // We cannot add a header as the head has already be written
            // For HTTP/1 and HTTP/1.1, the advertisement is done using a header, so we need to check
            // if we can write it. For HTTP/2 it's a custom frame.
            if (rc.request().version() != HttpVersion.HTTP_2 && !rc.response().headWritten()) {
                int port = rc.request().localAddress().port();
                String advertisement = "h3=\":" + port + "\"; ma=" + maxAgeSeconds;
                try {
                    rc.response().writeAltSvc(advertisement)
                            .onFailure(t -> LOG.debug("Failed to write Alt-Svc advertisement", t))
                            .onComplete(x -> rc.next());
                } catch (Exception e) {
                    // When using HTTP/1, writeAltSvc is synchronous.
                    // This catch block is just in case we pass the previous "head not written" condition, but it got
                    // written in the meantime (because writes are async)
                    LOG.debugf(e, "Failed to write Alt-Svc advertisement");
                    rc.next();
                }
            } else {
                // Too late to add the header
                LOG.debug("Failed to write Alt-Svc advertisement, head already written");
                rc.next();
            }
        } else {
            rc.next();
        }
    }
}
