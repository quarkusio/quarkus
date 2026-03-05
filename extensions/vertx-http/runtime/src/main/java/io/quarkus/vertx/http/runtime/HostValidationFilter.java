package io.quarkus.vertx.http.runtime;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;

import io.vertx.core.Handler;
import io.vertx.core.net.HostAndPort;
import io.vertx.ext.web.RoutingContext;

public class HostValidationFilter implements Handler<RoutingContext> {

    private static final Logger LOG = Logger.getLogger(HostValidationFilter.class);
    private static final int MAX_LOGGED_HOST_LENGTH = 30;

    private final Set<String> allowedHosts;

    public HostValidationFilter(Set<String> allowedHosts) {
        Objects.requireNonNull(allowedHosts);
        if (allowedHosts.isEmpty()) {
            throw new IllegalStateException("A set of allowed hosts can not be empty");
        }
        this.allowedHosts = Collections
                .unmodifiableSet(allowedHosts.stream().map(String::toLowerCase).collect(Collectors.toSet()));
    }

    @Override
    public void handle(RoutingContext context) {
        HostAndPort authority = context.request().authority();
        if (authority == null || authority.host() == null) {
            LOG.debug("Request HTTP Host header is not available, Host Validation filter requires a valid HTTP Host header");
            endResponse(context);
            return;
        }

        if (!allowedHosts.contains(authority.host().toLowerCase())) {
            if (LOG.isDebugEnabled()) {
                String logMessage = """
                        Request host %s rejected by the Host Validation filter""".formatted(checkHostLength(authority.host()));
                LOG.debug(logMessage);
            }
            endResponse(context);
            return;
        }

        context.next();
    }

    private static String checkHostLength(String host) {
        return host.length() > MAX_LOGGED_HOST_LENGTH ? (host.substring(0, MAX_LOGGED_HOST_LENGTH) + "...") : host;
    }

    private static void endResponse(RoutingContext context) {
        context.response().setStatusCode(403);
        context.response().end();
    }
}
