package io.quarkus.observability.promql.client.rest;

import java.util.Map;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;

import org.jboss.logging.Logger;

public class RequestDebugFilter implements ClientRequestFilter {
    private static final Logger log = Logger.getLogger(RequestDebugFilter.class.getPackageName() + ".>>>");

    @Override
    public void filter(ClientRequestContext requestContext) {
        if (log.isDebugEnabled()) {
            log.debugf("%s %s", requestContext.getMethod(), requestContext.getUri());
            requestContext
                    .getHeaders()
                    .entrySet()
                    .stream()
                    .flatMap(e -> e.getValue().stream().map(v -> Map.entry(e.getKey(), v)))
                    .forEach(e -> log.debugf("%s: %s", e.getKey(), e.getValue()));
            log.debug("");
            log.debugf("(%s): %s", requestContext.getEntityClass(), requestContext.getEntity());
        }
    }
}
