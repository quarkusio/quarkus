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
            int port = rc.request().localAddress().port();
            String advertisement = "h3=\":" + port + "\"; ma=" + maxAgeSeconds;
            rc.response().writeAltSvc(advertisement)
                    .onFailure(t -> LOG.warn("Failed to write Alt-Svc advertisement", t))
                    .onComplete(x -> rc.next());
        } else {
            rc.next();
        }
    }
}
