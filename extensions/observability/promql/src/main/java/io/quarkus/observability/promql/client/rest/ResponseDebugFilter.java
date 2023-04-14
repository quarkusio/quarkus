package io.quarkus.observability.promql.client.rest;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientResponseContext;
import jakarta.ws.rs.client.ClientResponseFilter;

import org.jboss.logging.Logger;

public class ResponseDebugFilter implements ClientResponseFilter {
    private static final Logger log = Logger.getLogger(ResponseDebugFilter.class.getPackageName() + ".<<<");

    @Override
    public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext) {
        if (log.isDebugEnabled()) {
            log.debugf("%s %s", responseContext.getStatusInfo().getStatusCode(), responseContext.getStatusInfo().toEnum());
            var headers = responseContext.getHeaders();
            if (headers != null) {
                headers
                        .entrySet()
                        .stream()
                        .flatMap(e -> e.getValue().stream().map(v -> Map.entry(e.getKey(), v)))
                        .forEach(e -> log.debugf("%s: %s", e.getKey(), e.getValue()));
            }
            log.debug("");
            var entityStream = responseContext.getEntityStream();
            if (entityStream != null) {
                responseContext.setEntityStream(
                        new DebugInputStream(
                                entityStream,
                                log::debug,
                                StandardCharsets.UTF_8));
            }
        }
    }
}
