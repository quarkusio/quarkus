package io.quarkus.vertx.http.runtime;

import java.util.Set;

import org.jboss.logging.Logger;

import io.vertx.core.Handler;
import io.vertx.core.net.HostAndPort;
import io.vertx.ext.web.RoutingContext;

public class HostValidationFilter implements Handler<RoutingContext> {

    private static final Logger LOG = Logger.getLogger(HostValidationFilter.class);

    private final Set<String> allowedHosts;

    public HostValidationFilter(Set<String> allowedHosts) {
        this.allowedHosts = allowedHosts;
    }

    @Override
    public void handle(RoutingContext context) {
        HostAndPort authority = context.request().authority();
        if (!allowedHosts.contains(authority.host())) {
            LOG.debugf("Invalid host %s", authority.host());
            context.response().setStatusCode(403);
            context.response().end();
            return;
        }

        context.next();
    }
}
