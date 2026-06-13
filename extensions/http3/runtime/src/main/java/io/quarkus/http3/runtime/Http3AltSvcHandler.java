package io.quarkus.http3.runtime;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpVersion;
import io.vertx.ext.web.RoutingContext;

public class Http3AltSvcHandler implements Handler<RoutingContext> {

    @Override
    public void handle(RoutingContext rc) {
        if (rc.request().isSSL() && rc.request().version() != HttpVersion.HTTP_3) {
            int port = rc.request().localAddress().port();
            rc.response().putHeader("Alt-Svc", "h3=\":" + port + "\"; ma=86400");
        }
        rc.next();
    }
}
